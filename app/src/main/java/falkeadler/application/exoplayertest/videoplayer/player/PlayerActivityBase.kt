package falkeadler.application.exoplayertest.videoplayer.player

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Rational
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.PlayerApplication
import falkeadler.application.exoplayertest.videoplayer.databinding.ActivityPlayerBinding
import falkeadler.application.exoplayertest.videoplayer.player.viewmodel.PlayerViewModel
import falkeadler.application.exoplayertest.videoplayer.player.customviews.VideoControllerView
import kotlinx.coroutines.*

abstract class PlayerActivityBase: AppCompatActivity(),
AudioManager.OnAudioFocusChangeListener,
Player.Listener,
VideoControllerView.OnControllerButtonsClickListener,
VideoControllerView.OnControllerVisibilityChangeListener{
    protected lateinit var player: ExoPlayer
        private set
    protected lateinit var binding: ActivityPlayerBinding
        private set
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private var defaultSystemVisibility: Int = -1

    private var isFullscreen: Boolean = false

    protected lateinit var playerViewModel: PlayerViewModel

    protected var afState = AudioFocusState.LOSS
        private set
    private lateinit var audioManager: AudioManager

    protected lateinit var mediaSession: MediaSessionCompat

    protected lateinit var oldIntent: Intent
    private var onceEnterPipMode = false;

    protected val MEDIA_ACTION_PLAY_PAUSE = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE
    protected val MEDIA_ACTION_ALL = MEDIA_ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    protected val MEDIA_ACION_PLAY_PAUSE_NEXT = MEDIA_ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
    protected val MEDIA_ACION_PLAY_PAUSE_PRVIOUS = MEDIA_ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

    private val mySessionCallback by lazy {
        buildSessionCallback()
    }

    //audio focus
    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_MEDIA)
                setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                build()
            })
            setAcceptsDelayedFocusGain(true)
            setOnAudioFocusChangeListener(this@PlayerActivityBase)
            build()
        }
    }

    //player callback
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

    abstract override fun onPlaybackStateChanged(playbackState: Int)

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        binding.playController.playingStateChanged(isPlaying)
        super.onIsPlayingChanged(isPlaying)
    }

    abstract override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int)

    abstract override fun onVideoSizeChanged(videoSize: VideoSize)

    // playControllers
    abstract override fun onPlayPauseClicked()

    abstract override fun onFastForwardClicked()

    abstract override fun onRewindClicked()

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        L.e(error.errorCodeName)
    }

    override fun onPlayerErrorChanged(error: PlaybackException?) {
        super.onPlayerErrorChanged(error)
        L.e(error?.errorCodeName ?: "")
    }

    override fun onVisibilityChange(visible: Boolean) {
        if (visible) {
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

    // lifecycle
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        }
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
        player.addListener(this)
        binding.playerView.player = player
        windowInsetsController = WindowCompat.getInsetsController(window, binding.playController)
        binding.playController.setOnControllerButtonsClickListener(this)
        binding.playController.setOnControllerVisibilityChangeListener(this)
        mediaSession = MediaSessionCompat(this, L.TAG).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS) // 얘는 기본적으로 됨
            setMediaButtonReceiver(null)
            setCallback(mySessionCallback)
        }

        binding.playController.setOnSeekbarChangeListener(object: OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                player.seekTo(binding.playController.convertProgressToSeekPosition(player.duration))
            }
        })
        handleIntent(intent = this.intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    abstract fun handleIntent(intent: Intent?)
    abstract fun updatePlaybackStateForMediaSession()
    abstract fun buildSessionCallback(): MediaSessionCompat.Callback

    override fun onStart() {
        super.onStart()
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

    override fun onPostResume() {
        super.onPostResume()
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
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

    override fun onStop() {
        super.onStop()
        player.stop()
        mySessionCallback.onStop()
        mediaSession.release()
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT < 26) {
            audioManager.abandonAudioFocus(this)
        } else {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
        stopTimer2()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.playerView.player = null
        player.stop()
        player.release()
        player.removeListener(this)
    }

    override fun finish() {
        if (onceEnterPipMode) {
            L.e("once enter in pipMode")
            (application as PlayerApplication).launcherToFront()
        } else {
            super.finish()
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
}