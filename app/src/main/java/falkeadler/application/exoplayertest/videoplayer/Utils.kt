package falkeadler.application.exoplayertest.videoplayer

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.util.Log
import android.util.Size
import android.widget.TextView
import kotlinx.coroutines.*
import java.io.File

object L {
    const val TAG = "VideoPlayerTestTAG"
    fun e(msg: String) {
        Log.e(TAG, buildMessage(msg))
    }

    private fun buildMessage(msg: String): String {

        return StringBuilder().apply {
            val stackElement = Thread.currentThread().stackTrace[4]
            append("[").append(stackElement.fileName).append(":")
                .append(stackElement.methodName).append("#")
                .append(stackElement.lineNumber).append("]")
                .append(msg)
        }.toString()
    }
}

fun TextView.setDurationText(time: Long, justSec: Boolean = false) {
    val sec = if (justSec) time else time / 1000
    val s = sec % 60
    val m = (sec / 60) % 60
    val h = (sec / (60 * 60))
    text = String.format("%d:%02d:%02d", h, m, s)
}

fun TextView.setRuntimeText(min: Int) {
    val h = min / 60
    val m = min % 60
    text = if (h == 0) {
        "$m min"
    } else {
        String.format("%d hour %02d min", h, m)
    }
}