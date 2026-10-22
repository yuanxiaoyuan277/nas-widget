package com.yxy.naswidget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/** 迷你趋势折线图：可绘制多条近期走势（资源/温度/网络等），点击宿主决定显示哪组数据 */
class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 一条趋势序列：颜色 + 数值 + 固定刻度上限（<=0 表示按自身最大值自动缩放） */
    data class Series(val color: Int, val values: List<Float>, val scaleMax: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var seriesList: List<Series> = emptyList()

    fun setSeries(series: List<Series>) {
        seriesList = series
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        paint.strokeWidth = 2f * resources.displayMetrics.density
        for (s in seriesList) {
            paint.color = s.color
            drawSeries(canvas, s.values, w, h, s.scaleMax)
        }
    }

    private fun drawSeries(canvas: Canvas, values: List<Float>, w: Float, h: Float, fixedMax: Float) {
        if (values.size < 2) return
        // 固定刻度：百分比/温度用固定上限（如 100），避免线条上下抖动；网络速率按峰值自动缩放
        val max = if (fixedMax > 0f) fixedMax else (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val pad = 2f * resources.displayMetrics.density
        val stepX = (w - pad * 2) / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = pad + i * stepX
            val y = pad + (h - pad * 2) * (1f - v.coerceIn(0f, max) / max)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }
}
