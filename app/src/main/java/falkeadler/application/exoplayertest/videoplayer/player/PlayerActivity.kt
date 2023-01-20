
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
import androidx.core.view.forEach
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.video.VideoSize
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.PlayerApplication
import falkeadler.application.exoplayertest.videoplayer.R
import falkeadler.application.exoplayertest.videoplayer.databinding.ActivityPlayerBinding
import falkeadler.application.exoplayertest.videoplayer.player.viewmodel.PlayerViewModel
import falkeadler.application.exoplayertest.videoplayer.player.customviews.VideoControllerView
import falkeadler.application.exoplayertest.videoplayer.player.viewmodel.ContentsType
import falkeadler.library.youtubedataextractor.YouTubeData
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class PlayerActivity : AppCompatActivity(), AudioManager.OnAudioFocusChangeListener{
    private lateinit var player: ExoPlayer
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private var defaultSystemVisibility: Int = -1

    private var isFullscreen: Boolean = false

    private lateinit var playerViewModel: PlayerViewModel

    private var afState = AudioFocusState.LOSS
    private lateinit var audioManager: AudioManager

    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var oldIntent: Intent
    private var onceEnterPipMode = false;

    private val MEDIA_ACTION_PLAY_PAUSE = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE
    private val MEDIA_ACTION_ALL = MEDIA_ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    private val MEDIA_ACION_PLAY_PAUSE_NEXT = MEDIA_ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
    private val MEDIA_ACION_PLAY_PAUSE_PRVIOUS = MEDIA_ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

    private lateinit var cacheManager: PlayerApplication.CacheManger

    private var currentContentsType: ContentsType = ContentsType.Local

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
        cacheManager = PlayerApplication.CacheManger.getInstance(this)
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
        processIntent(intent)
        player.addListener(playerListener)
        binding.playerView.player = player
        player.prepare()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            binding.playController.bufferingStateChanged(false)
            if (playbackState == Player.STATE_ENDED) {
                if (player.currentMediaItemIndex == player.mediaItemCount - 1) {
                    finish()
                }
            }
            if (playbackState == Player.STATE_BUFFERING) {
                L.e("buffering : ${player.bufferedPercentage}%")
                binding.playController.bufferingStateChanged(true)
                binding.playController.seekBarSecondaryProgress = player.bufferedPercentage
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            binding.playController.playingStateChanged(isPlaying)
            updatePlaybackStateForMediaSession()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            L.e("title = ${mediaItem?.mediaMetadata?.title}")
            mediaItem?.mediaMetadata?.title?.let {
                title ->
                supportActionBar?.title = title
                val mediaMetadataCompat = MediaMetadataCompat.Builder().putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    title.toString()
                ).build()
                mediaSession.setMetadata(mediaMetadataCompat)
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            try {
                val param = PictureInPictureParams.Builder()
                param.setAspectRatio(Rational(videoSize.width, videoSize.height))
                setPictureInPictureParams(param.build())
            } catch (e: java.lang.IllegalArgumentException) {
                val param = PictureInPictureParams.Builder()
                param.setAspectRatio(Rational(16, 9))
                setPictureInPictureParams(param.build())
            } catch (e: IllegalArgumentException) {
                val param = PictureInPictureParams.Builder()
                param.setAspectRatio(Rational(16, 9))
                setPictureInPictureParams(param.build())
            }
        }
    }


    private fun processIntent(intent: Intent?) {
        intent?.run {
            if (this@PlayerActivity::oldIntent.isInitialized) {
                player.stop()
                player.release()
                player.removeListener(playerListener)
                binding.playerView.player = null
            }
            oldIntent = this
            intent.data?.let {
                uri ->
                val trackSelector = DefaultTrackSelector(this@PlayerActivity).apply {
                    setParameters(buildUponParameters().setMaxVideoSize(1280, 720))
                }
                player = ExoPlayer.Builder(this@PlayerActivity)
                    .setMediaSourceFactory(cacheManager.createMediaSourceFactory())
                    .setTrackSelector(trackSelector)
                    .build()
                if ((uri.scheme == "content" && uri.toString().startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())) ||
                            uri.scheme == "file") {
                    val metadata = if (uri.scheme == "file") {
                        MediaMetadata.Builder().setTitle(uri.lastPathSegment).build()
                    } else {
                        val title = contentResolver.query(uri, null, null, null, null).use {
                            cursor ->
                            val value =
                            cursor?.let {
                                it.moveToFirst()
                                it.getString(it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.TITLE))
                            } ?: ""
                            value
                        }
                        MediaMetadata.Builder().setTitle(title).build()
                    }
                    val mediaItem = MediaItem.fromUri(uri).buildUpon().setMediaMetadata(metadata).build()
                    player.addMediaItem(mediaItem)
                    currentContentsType = ContentsType.Local
                } else if (uri.scheme == "https") {
                    //streaming
                    if (intent.hasExtra("YOUTUBEDATA")) {
                        intent.getStringExtra("YOUTUBEDATA")?.let {
                            ytdatastr ->
                            val parser = Json {
                                isLenient = true
                                coerceInputValues = true
                                ignoreUnknownKeys = true
                            }
                            val data = parser.decodeFromString<YouTubeData>(ytdatastr)
                            val metadata = MediaMetadata.Builder().setTitle(data.title).build()
                            if (data.isLiveContent) {
                                if (data.hlsStream.isNotEmpty()) {
                                    val selected = data.hlsStream.maxBy {
                                        it.height * it.width * it.audioSampleRate.toLong()
                                    }
                                    MediaItem.fromUri(selected.url).buildUpon().setMediaMetadata(metadata).build()
                                    val source = cacheManager.buildHLSMediaSource(selected.url)
                                    player.setMediaSource(source)
                                } else {
                                    val selectedVideo = data.videos.maxBy { it.height * it.width }
                                    val videoItem = MediaItem.fromUri(selectedVideo.url).buildUpon().setMediaMetadata(metadata).build()
                                    val selectedAudio = data.audios.maxBy { it.audioSampleRate }
                                    val audioItem = MediaItem.fromUri(selectedAudio.url).buildUpon().setMediaMetadata(metadata).build()
                                    val mergeSource = MergingMediaSource(true,
                                    cacheManager.buildDASHMediaSource(videoItem),
                                    cacheManager.buildDASHMediaSource(audioItem))
                                    player.setMediaSource(mergeSource, true)
                                }
                            } else {
                                val videoSource = MediaItem.fromUri(uri).buildUpon().setMediaMetadata(metadata).build()
                                val video = cacheManager.buildProgressiveMediaSource(videoSource)
                                val audioSource = data.audios.filter { it.itag == 140 }.map { MediaItem.fromUri(it.url) }.firstOrNull() ?:
                                run {
                                    val max = data.audios.maxBy { it.audioSampleRate }
                                    MediaItem.fromUri(max.url)
                                }
                                val audio = cacheManager.buildProgressiveMediaSource(audioSource)
                                val merged = MergingMediaSource(true, video, audio)
                                player.setMediaSource(merged)
                            }
                        }
                        currentContentsType = ContentsType.YouTube
                    } else {
                        // test!!!
                        if (uri.lastPathSegment?.endsWith("m3u8") == true) {
                            val mediaSource = cacheManager.buildHLSMediaSource(uri.toString())
                            player.setMediaSource(mediaSource)
                        } else if (uri.lastPathSegment?.endsWith("mp4") == true) {
                            val mediaSource =
                                cacheManager.buildProgressiveMediaSource(uri.toString())
                            player.setMediaSource(mediaSource)
                        } else {
                            val mediaSource = cacheManager.buildDASHSMediaSource(uri.toString())
                            player.setMediaSource(mediaSource)
                        }
                        currentContentsType = ContentsType.Streaming
                    }
                }
            }
        } ?: finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
        player.addListener(playerListener)
        binding.playerView.player = player
        player.prepare()
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

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (currentContentsType == ContentsType.Local) {
            menu?.forEach {
                it.isEnabled = it.itemId == R.id.searchTmdb
            }
        } else if (currentContentsType == ContentsType.YouTube) {
            menu?.forEach {
                it.isEnabled = it.itemId == R.id.showYoutubeInfo
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_player, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.searchTmdb -> {
                // search tmdb and start bottomsheet for ac
                player.currentMediaItem?.mediaMetadata?.title?.let {
                    val frag = MovieDetailBottomSheet(it.toString())
                    frag.show(supportFragmentManager, MovieDetailBottomSheet.TAG)
                }
            }
            R.id.showYoutubeInfo -> {

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