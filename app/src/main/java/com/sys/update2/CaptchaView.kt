package com.sys.update2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class CaptchaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var code: String = ""
        private set

    private val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val cols = intArrayOf(
        Color.parseColor("#27ae60"),
        Color.parseColor("#2980b9"),
        Color.parseColor("#8e44ad"),
        Color.parseColor("#c0392b"),
        Color.parseColor("#16a085")
    )

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        textSkewX = -0.15f   // ميلان بسيط يحاكي خط cursive
    }

    private data class CapChar(val ch: Char, val color: Int, val size: Float, val rot: Float)
    private var items = listOf<CapChar>()
    private var noise = listOf<FloatArray>()

    init { regenerate() }

    fun regenerate() {
        val d = resources.displayMetrics.density
        val sb = StringBuilder()
        val list = mutableListOf<CapChar>()
        repeat(4) {
            val c = chars[Random.nextInt(chars.length)]
            sb.append(c)
            list.add(
                CapChar(
                    ch = c,
                    color = cols[Random.nextInt(cols.size)],
                    size = (20 + Random.nextFloat() * 6) * d,
                    rot = ((Random.nextFloat() - 0.5f) * 0.7f) * (180f / Math.PI.toFloat())
                )
            )
        }
        code = sb.toString()
        noise = List(3) {
            floatArrayOf(
                Random.nextFloat(), Random.nextFloat(),
                Random.nextFloat(), Random.nextFloat(),
                Random.nextInt(150).toFloat(),
                Random.nextInt(150).toFloat(),
                Random.nextInt(150).toFloat()
            )
        }
        items = list
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()

        // خلفية بيضا بحواف دائرية 7px
        canvas.drawRoundRect(RectF(0f, 0f, w, h), 7 * d, 7 * d, bgPaint)

        // 3 خطوط تشويش عشوائية
        for (n in noise) {
            linePaint.color = Color.argb(153, n[4].toInt(), n[5].toInt(), n[6].toInt())
            canvas.drawLine(n[0] * w, n[1] * h, n[2] * w, n[3] * h, linePaint)
        }

        // 4 حروف — نفس أماكن الـ HTML: x = 10+i*19 ، y = 26±3 ، دوران عشوائي
        items.forEachIndexed { i, it ->
            textPaint.color = it.color
            textPaint.textSize = it.size
            val x = (10 + i * 19) * d
            val y = (26 + Random.nextFloat() * 5 - 2) * d
            canvas.save()
            canvas.rotate(it.rot, x, y)
            canvas.drawText(it.ch.toString(), x, y, textPaint)
            canvas.restore()
        }
    }
}
