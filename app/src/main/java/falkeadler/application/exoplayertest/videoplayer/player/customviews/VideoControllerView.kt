package falkeadler.application.exoplayertest.videoplayer.player.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.R
import falkeadler.application.exoplayertest.videoplayer.databinding.VideoControllerViewBinding

@SuppressLint("ClickableViewAccessibility")
class VideoControllerView @JvmOverloads
constructor(context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0,
            defStyleRes: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    interface OnControllerButtonsClickListener {
        fun onPlayPauseClicked()
        fun onFastForwardClicked()
        fun onRewindClicked()
    }

    interface OnControllerVisibilityChangeListener {
        fun onVisibilityChange(visible: Boolean)
    }

    private val binding: VideoControllerViewBinding by lazy {
        VideoControllerViewBinding.inflate(LayoutInflater.from(getContext()), this, true)
    }
    private var buttonClickListener: OnControllerButtonsClickListener? = null
    private var seekBarChangeListener: SeekBar.OnSeekBarChangeListener? =  null
    private var visibilityChangeListener: OnControllerVisibilityChangeListener? = null

    init {
        binding.progressLayout.seekbar
        binding.progressLayout.seekbar.apply {
            max = 1000
        }.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                seekBarChangeListener?.onProgressChanged(p0, p1, p2)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {
                seekBarChangeListener?.onStartTrackingTouch(p0)
            }
            override fun onStopTrackingTouch(p0: SeekBar?) {
                seekBarChangeListener?.onStopTrackingTouch(p0)
            }
        })
        binding.playPause.setOnClickListener {
            buttonClickListener?.onPlayPauseClicked()
        }

        binding.fastForward.setOnClickListener {
            buttonClickListener?.onFastForwardClicked()
        }

        binding.rewind.setOnClickListener {
            buttonClickListener?.onRewindClicked()
        }

        binding.touchProcessorStart.setOnTouchListener { view, p1 ->
            if (p1.action == MotionEvent.ACTION_DOWN) {
                view.isPressed = true
            } else if (p1.action == MotionEvent.ACTION_UP || p1.action == MotionEvent.ACTION_CANCEL) {
                view.isPressed = false
            }
            startGestureListener.onTouchEvent(p1)
            true
        }
        binding.touchProcessorEnd.setOnTouchListener { view, p1 ->
            if (p1.action == MotionEvent.ACTION_DOWN) {
                view.isPressed = true
            } else if (p1.action == MotionEvent.ACTION_UP || p1.action == MotionEvent.ACTION_CANCEL) {
                view.isPressed = false
            }
            endGestureListener.onTouchEvent(p1)
            true
        }
    }

    fun show() {
        binding.controlLayout.visibility = View.VISIBLE
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, 3000)
        visibilityChangeListener?.onVisibilityChange(true)
    }

    fun hideImmediately() {
        handler.removeCallbacks(hideRunnable)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            hideRunnable.run()
        } else {
            handler.post(hideRunnable)
        }
    }

    fun hide() {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, 3000)
    }

    private val hideRunnable = Runnable {
        handler.run {
            L.e("hide contoller?!!!1")
            binding.controlLayout.visibility = View.GONE
            visibilityChangeListener?.onVisibilityChange(false)
        }
    }



    fun setOnControllerButtonsClickListener(cb : OnControllerButtonsClickListener) {
        this.buttonClickListener = cb
    }

    fun setOnSeekbarChangeListener(cb : SeekBar.OnSeekBarChangeListener) {
        this.seekBarChangeListener = cb
    }

    private fun convertMilliSecondToHHMMSS(time: Long): String {
        val sec = time / 1000
        val s = sec % 60
        val m = (sec / 60) % 60
        val h = (sec / (60 * 60))
        return String.format("%d:%02d:%02d", h, m, s)
    }


    fun setDuration(duration: Long) {
        binding.progressLayout.totalTime.text = convertMilliSecondToHHMMSS(duration)
    }

    fun setProgress(current: Long, duration: Long) {
        setDuration(duration)
        binding.progressLayout.currentTime.text = convertMilliSecondToHHMMSS(current)
        val position = ((current.toDouble() / duration.toDouble()) * 1000).toInt()
        binding.progressLayout.seekbar.progress = position
    }

    fun convertProgressToSeekPosition(duration: Long): Long {
        return (duration * ((binding.progressLayout.seekbar.progress) / 1000.0)).toLong()
    }

    private val startGestureListener = GestureDetector(getContext(), SimpleGestureDetectorWithDirection(true))
    private val endGestureListener = GestureDetector(getContext(), SimpleGestureDetectorWithDirection(false))

    inner class SimpleGestureDetectorWithDirection(private val isLeft: Boolean): SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            show()
            return true
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            if (isLeft) {
                buttonClickListener?.onRewindClicked()
            } else {
                buttonClickListener?.onFastForwardClicked()
            }
            return true
        }
    }

    fun playingStateChanged(isPlaying: Boolean) {
        if (isPlaying) {
            binding.playPause.setImageResource(R.drawable.pause)
        } else {
            binding.playPause.setImageResource(R.drawable.play)
        }
    }

    fun setOnControllerVisibilityChangeListener(cb : OnControllerVisibilityChangeListener) {
        visibilityChangeListener = cb
    }

    fun setOnControllerVisibilityChangeListener(cb : (Boolean) -> Unit) {
        visibilityChangeListener = object : OnControllerVisibilityChangeListener {
            override fun onVisibilityChange(visible: Boolean) {
                cb(visible)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        hide()
    }

    fun bufferingStateChanged(isBuffering: Boolean) {
        if (isBuffering) {
            binding.bufferingContents.show()
        } else {
            binding.bufferingContents.hide()
        }
    }
}