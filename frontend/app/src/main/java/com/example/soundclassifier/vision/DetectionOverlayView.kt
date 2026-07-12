package com.example.soundclassifier.vision

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/** Draws bounding boxes + labels on top of the camera preview. */
class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var results: List<DetectedObject> = emptyList()
    private var scaleFactor = 1f
    private var imageWidth = 1
    private var imageHeight = 1

    private val boxPaint = Paint().apply {
        color = Color.parseColor("#4D9DE0")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val textBgPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        alpha = 160
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 42f
    }

    fun setResults(detections: List<DetectedObject>, imgHeight: Int, imgWidth: Int) {
        results = detections
        imageHeight = imgHeight
        imageWidth = imgWidth
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        invalidate()
    }

    fun clear() {
        results = emptyList()
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        for (result in results) {
            val box = result.box
            val rect = RectF(
                box.left * scaleFactor,
                box.top * scaleFactor,
                box.right * scaleFactor,
                box.bottom * scaleFactor
            )
            canvas.drawRect(rect, boxPaint)

            val label = "${result.label} ${(result.score * 100).toInt()}%"
            val textWidth = textPaint.measureText(label)
            canvas.drawRect(
                rect.left,
                rect.top - 52f,
                rect.left + textWidth + 16f,
                rect.top,
                textBgPaint
            )
            canvas.drawText(label, rect.left + 8f, rect.top - 12f, textPaint)
        }
    }
}
