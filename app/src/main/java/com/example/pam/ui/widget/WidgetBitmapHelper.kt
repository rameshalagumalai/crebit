package com.example.pam.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.pam.R

object WidgetBitmapHelper {

    private var dmSansTypeface: Typeface? = null
    private var dmSansBoldTypeface: Typeface? = null

    private fun getTypeface(context: Context, isBold: Boolean): Typeface {
        if (dmSansTypeface == null) {
            try {
                dmSansTypeface = ResourcesCompat.getFont(context, R.font.dm_sans)
            } catch (_: Exception) {}
        }
        val base = dmSansTypeface ?: Typeface.DEFAULT
        return if (isBold) {
            if (dmSansBoldTypeface == null) {
                dmSansBoldTypeface = Typeface.create(base, Typeface.BOLD)
            }
            dmSansBoldTypeface ?: Typeface.DEFAULT_BOLD
        } else {
            base
        }
    }

    fun createTextBitmap(
        context: Context,
        text: String,
        textSizeSp: Float,
        textColor: Int,
        isBold: Boolean = false
    ): Bitmap {
        val typeface = getTypeface(context, isBold)
        val density = context.resources.displayMetrics.density
        val px = textSizeSp * density

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.textSize = px
            this.color = textColor
            this.typeface = typeface
        }

        val fontMetrics = paint.fontMetrics
        val baseline = -fontMetrics.ascent
        val width = (paint.measureText(text) + 2f).toInt().coerceAtLeast(1)
        val height = (baseline + fontMetrics.descent + 2f).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText(text, 1f, baseline, paint)
        return bitmap
    }
}
