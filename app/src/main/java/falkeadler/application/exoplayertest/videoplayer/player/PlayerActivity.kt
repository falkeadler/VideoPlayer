/*
이 코드는 플레이어 액티비티 초기 코드로 일단 그냥 둡니다.
 */
package falkeadler.application.exoplayertest.videoplayer.player

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Rational
import android.view.*
import android.widget.SeekBar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.PlayerApplication
import falkeadler.application.exoplayertest.videoplayer.R
import falkeadler.application.exoplayertest.videoplayer.databinding.ActivityPlayerBinding
import falkeadler.application.exoplayertest.videoplayer.player.viewmodel.PlayerViewModel
import falkeadler.application.exoplayertest.videoplayer.player.viewmodel.VideoData
import falkeadler.application.exoplayertest.videoplayer.player.customviews.VideoControllerView
import kotlinx.coroutines.*

class PlayerActivity : AppCompatActivity(), AudioManager.OnAudioFocusChangeListener{
    private lateinit var player: ExoPlayer
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private var defaultSystemVisibility: Int = -1

    private var isFullscreen: Boolean = false

    private lateinit var playerViewModel: PlayerViewModel

    private var playOnlyOne = false

    private var afState = AudioFocusState.LOSS
    private lateinit var audioManager: AudioManager

    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var oldIntent: Intent
    private var onceEnterPipMode = false;

    private val MEDIA_ACTION_PLAY_PAUSE = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE
    private val MEDIA_ACTION_ALL = MEDIA_ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    private val MEDIA_ACION_PLAY_PAUSE_NEXT = MEDIA_ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
    private val MEDIA_ACION_PLAY_PAUSE_PRVIOUS = MEDIA_ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_MEDIA)
                setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                build()
            })
            setAcceptsDelayedFocusGain(true)
            setOnAudioFocusChangeListener(this@PlayerActivity)
            build()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        }
        window.decorView.setBackgroundColor(Color.CYAN)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setSupportActionBar(binding.controllerToolbar)

        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setShowHideAnimationEnabled(true)

        binding.appbarLayout.outlineProvider = null
        playerViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[PlayerViewModel::class.java]
        isFullscreen = true
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        player = ExoPlayer.Builder(this).build()
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_ENDED) {
                        if (player.currentMediaItemIndex == player.mediaItemCount - 1 || playOnlyOne) {
                            finish()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    binding.playController.playingStateChanged(isPlaying)
                    updatePlaybackStateForMediaSession()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    mediaItem?.localConfiguration?.tag?.let {
                        if (it is VideoData) {
                            supportActionBar?.title = it.title
                            if (this@PlayerActivity::mediaSession.isInitialized) {
                                val metaData = MediaMetadataCompat.Builder().putString(
                                    MediaMetadataCompat.METADATA_KEY_TITLE,
                                    it.title
                                ).build()
                                mediaSession.setMetadata(metaData)
                            }
                        }
                    }
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    super.onVideoSizeChanged(videoSize)
                    val param = PictureInPictureParams.Builder()
                    param.setAspectRatio(Rational(videoSize.width, videoSize.height))
                    setPictureInPictureParams(param.build())
                }

            }
        )

        binding.playerView.player = player

        windowInsetsController = WindowCompat.getInsetsController(window, binding.playController)

        binding.playController.setOnControllerButtonsClickListener(object : VideoControllerView.OnControllerButtonsClickListener {
            override fun onPlayPauseClicked() {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }

            override fun onFastForwardClicked() {
                player.seekForward()
            }

            override fun onRewindClicked() {
                player.seekBack()
            }
        })

        binding.playController.setOnSeekbarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                player.seekTo(binding.playController.convertProgressToSeekPosition(player.duration))
            }
        })

        binding.playController.setOnControllerVisibilityChangeListener {
            if (it) {
                supportActionBar?.show()
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    window.decorView.systemUiVisibility = defaultSystemVisibility
                }
                isFullscreen = false
            } else {
                supportActionBar?.hide()
                if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    defaultSystemVisibility = window.decorView.systemUiVisibility
                    window.decorView.systemUiVisibility =
                        (View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                } else {
                    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
                isFullscreen = true
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        L.e("[PIPTEST] onNewIntent????")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            oldIntent = it
            if (player.isPlaying) {
                player.stop()
                player.clearMediaItems()
            }
            it.data?.let {
                    data ->
                it.getStringExtra(MediaStore.Video.Media.BUCKET_ID)?.let { bucketId ->
                    playOnlyOne = false
                    lifecycleScope.launch {
                        val (list, index) = playerViewModel.buildLocalList(data, bucketId)
                        player.addMediaItems(list)
                        player.seekToDefaultPosition(index)
                        player.prepare()
                    }
                } ?: run {
                    playOnlyOne = true
                    val item = MediaItem.Builder().setUri(data)
                        .setTag(playerViewModel.queryMediaData(data))
                        .setMediaId(data.lastPathSegment!!).build()
                    player.addMediaItem(item)
                    player.prepare()
                }
            }
        } ?: finish()
    }

    override fun onStart() {
        super.onStart()
        // 어쨋든 여기가 실행됨.
        mediaSession = MediaSessionCompat(this, L.TAG).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS) // 얘는 기본적으로 됨
            setMediaButtonReceiver(null)
            setCallback(mySessionCallback)
        }

        mediaSession.isActive = true
        updatePlaybackStateForMediaSession()
        MediaControllerCompat.setMediaController(this, mediaSession.controller)
        val result = if (Build.VERSION.SDK_INT < 26) {
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        } else {
            audioManager.requestAudioFocus(audioFocusRequest)
        }
        when(result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                afState = AudioFocusState.GAINED
                player.playWhenReady = true
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                afState = AudioFocusState.DELAYED
                player.playWhenReady = false
            }
            else -> {
                afState = AudioFocusState.LOSS
                player.playWhenReady = false // 멈춰둘것
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startTimer2()
    }

    override fun onStop() {
        super.onStop()
        mySessionCallback.onPause()
        mediaSession.release()
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT < 26) {
            audioManager.abandonAudioFocus(this)
        } else {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
        stopTimer2()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        L.e("[PIPTEST] call mode  change $isInPictureInPictureMode, config = ")
        if (isInPictureInPictureMode) {
            onceEnterPipMode = true
            stopTimer2() // 타이머 처리를 위한
        } else {
            startTimer2()
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
        player.release()
        binding.playerView.player = null
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when(focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // 얻어옴
                if (afState != AudioFocusState.LOSS) {
                    player.playWhenReady = true
                }
            }
            else -> {
                if (player.playbackState == Player.STATE_READY) {
                    player.pause()// 뭘 하든 일단 pause는 때려야함. 포즈 때리고 죽진 않겟지?
                }
                player.playWhenReady = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_player, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.searchTmdb -> {
                // search tmdb and start bottomsheet for ac
                L.e("Call optionMenu")
                player.currentMediaItem?.localConfiguration?.tag?.let {
                    if (it is VideoData) {
                        val frag = MovieDetailBottomSheet(it)
                        frag.show(supportFragmentManager, MovieDetailBottomSheet.TAG)
                    }
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val mySessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPause() {
            super.onPause()
            player.pause()
        }

        override fun onPlay() {
            super.onPlay()
            player.playWhenReady = true
        }

        override fun onFastForward() {
            super.onFastForward()
            player.seekForward()
        }

        override fun onRewind() {
            super.onRewind()
            player.seekBack()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            player.seekToNextMediaItem()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            player.seekToPreviousMediaItem()
        }

        override fun onStop() {
            super.onStop()
            player.stop()
            player.playWhenReady = false
        }
    }

    private var timer: Job? = null
    private fun startTimer2() {
        synchronized(this) {
            timer?.let {
                it.cancel()
            }
            timer = null
            timer = lifecycleScope.launch(context = Dispatchers.Default) {
                while (true) {
                    delay(500L)
                    withContext(Dispatchers.Main) {
                        if (player.isPlaying) {
                            binding.playController.setProgress(
                                player.currentPosition,
                                player.duration
                            )
                        }
                    }
                }
            }
        }
    }
    private fun stopTimer2() {
        synchronized(this) {
            timer?.let {
                it.cancel()
            }
            timer = null
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun updatePlaybackStateForMediaSession() {
        if (mediaSession.isActive) {
            val stateBuilder = PlaybackStateCompat.Builder()
            if (player.mediaItemCount == 1) {
                stateBuilder.setActions(MEDIA_ACTION_PLAY_PAUSE)
            } else {
                if (player.currentMediaItemIndex == 0) {
                    stateBuilder.setActions(MEDIA_ACION_PLAY_PAUSE_NEXT)
                } else if (player.currentMediaItemIndex == player.mediaItemCount - 1) {
                    stateBuilder.setActions(MEDIA_ACION_PLAY_PAUSE_PRVIOUS)
                } else {
                    stateBuilder.setActions(MEDIA_ACTION_ALL)
                }
            }
            stateBuilder.setState(
                if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                0,
                1.0f
            )
            mediaSession.setPlaybackState(stateBuilder.build())
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when(keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                mediaController.dispatchMediaButtonEvent(event!!)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // 여기서 액티비티 이동을 하지는 않겠지?
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            val rational = if (player.videoSize.width  > 0 && player.videoSize.height > 0) {
                Rational(player.videoSize.width, player.videoSize.height)
            } else {
                Rational(1, 1)
            }
            val param = PictureInPictureParams.Builder().setAspectRatio(rational)
                .build()
            binding.playController.hideImmediately()
            enterPictureInPictureMode(param)
        }
    }

    override fun finish() {
        if (onceEnterPipMode) {
            L.e("[PIPTEST] is in pip = $isInPictureInPictureMode : called!!!!")
            (application as PlayerApplication).launcherToFront()
        } else {
            super.finish()
        }
    }
}