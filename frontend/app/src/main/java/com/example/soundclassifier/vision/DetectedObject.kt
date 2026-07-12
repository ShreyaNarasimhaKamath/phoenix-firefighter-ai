package com.example.soundclassifier.vision

import android.graphics.RectF

/** One detected object — our own type so UI/network code doesn't depend on the ML library. */
data class DetectedObject(
    val label: String,
    val score: Float,
    val box: RectF
)
