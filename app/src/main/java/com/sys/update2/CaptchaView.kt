package com.sys.update2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class CaptchaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val COLORS = intArrayOf(
        Color.parseColor("#27ae60"),
        Color.parseColor("#2980b9"),
        Color.parseColor("#8e44ad"),
        Color.parseColor("#c0392b"),
        Color.parseColor("#16a085")
    )

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
    }

    var code: String = ""
        private set

    init {
        regenerate()
    }

    fun regenerate() {
        val sb = StringBuilder()
        repeat(4) { sb.append(CHARS[Random.nextInt(CHARS.length)]) }
        code = sb.toString()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        try {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            canvas.drawRect(0f, 0f, w, h, bgPaint)

            repeat(3) {
                linePaint.color = Color.rgb(
                    Random.nextInt(150),
                    Random.nextInt(150),
                    Random.nextInt(150)
                )
                linePaint.alpha = 150
                canvas.drawLine(
                    Random.nextFloat() * w, Random.nextFloat() * h,
                    Random.nextFloat() * w, Random.nextFloat() * h,
                    linePaint
                )
            }

            textPaint.textSize = h * 0.55f
            for (i in code.indices) {
                textPaint.color = COLORS[Random.nextInt(COLORS.size)]
                val x = (w / code.length) * i + (w * 0.1f)
                val y = h * 0.7f + Random.nextInt(-5, 5)
                canvas.save()
                canvas.rotate(Random.nextInt(-15, 15).toFloat(), x, y)
                canvas.drawText(code[i].toString(), x, y, textPaint)
                canvas.restore()
            }
        } catch (_: Exception) {}
    }
}
