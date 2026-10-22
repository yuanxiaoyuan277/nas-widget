package com.yxy.naswidget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/** 迷你趋势折线图：同时绘制 CPU（蓝）与内存（绿）两条近期走势（0~100%） */
class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var cpuValues: List<Float> = emptyList()
    private var memValues: List<Float> = emptyList()

    fun setData(cpu: List<Float>, mem: List<Float>) {
        cpuValues = cpu
        memValues = mem
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        paint.strokeWidth = 2f * resources.displayMetrics.density

        paint.color = 0xFF58D68D.toInt() // 内存绿
        drawSeries(canvas, memValues, w, h)
        paint.color = 0xFF5E9EFF.toInt() // CPU 蓝
        drawSeries(canvas, cpuValues, w, h)
    }

    private fun drawSeries(canvas: Canvas, values: List<Float>, w: Float, h: Float) {
        if (values.size < 2) return
        val pad = 2f * resources.displayMetrics.density
        val stepX = (w - pad * 2) / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = pad + i * stepX
            val y = pad + (h - pad * 2) * (1f - v.coerceIn(0f, 100f) / 100f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }
}
