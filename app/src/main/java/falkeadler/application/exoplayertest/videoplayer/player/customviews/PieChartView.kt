package falkeadler.application.exoplayertest.videoplayer.player.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toRectF

class PieChartView @JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val paint = Paint()
    @androidx.annotation.IntRange(from = 0, to = 100)
    var progress: Int = 0

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        paint.strokeWidth = 6f
        paint.color = Color.YELLOW

        var rect = Rect()
        getDrawingRect(rect)
        var paddingRect = Rect(rect.left + paddingStart, rect.top + paddingTop, rect.right - paddingEnd, rect.bottom - paddingBottom)

        if (progress == 0) {
            paint.style = Paint.Style.STROKE
            canvas?.drawArc(paddingRect.toRectF(), 270f, 360f, true, paint)
        } else {
            val sweep = 360f * (progress.toFloat() / 100)
            paint.style = Paint.Style.FILL_AND_STROKE
            canvas?.drawArc(paddingRect.toRectF(), 270f, sweep, true, paint)
        }
    }
}