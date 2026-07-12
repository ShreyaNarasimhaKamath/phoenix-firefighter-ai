package com.example.soundclassifier.ml

import android.content.Context
import android.media.AudioRecord
import android.os.Handler
import android.os.Looper
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Loudness stats for one audio frame, used for proximity + noise estimation.
 *
 * @param rmsDb   frame energy in dBFS (0 = max, ~-80 = silence)
 * @param peakDb  loudest sample in dBFS
 */
data class AudioStats(val rmsDb: Float, val peakDb: Float)

/**
 * Milestone 1: all YamNet / microphone logic lives here,
 * so MainActivity only handles UI.
 *
 * Usage:
 *   val engine = YamNetEngine(context)
 *   engine.start { categories, stats -> ... }   // called every 500ms, sorted by score
 *   engine.stop()
 */
class YamNetEngine(context: Context) {

    companion object {
        private const val MODEL_FILE = "yamnet.tflite"
        private const val INTERVAL_MS = 500L
        private const val SILENCE_DB = -80f
    }

    private val classifier: AudioClassifier =
        AudioClassifier.createFromFile(context, MODEL_FILE)
    private val tensorAudio = classifier.createInputTensorAudio()
    private val record: AudioRecord = classifier.createAudioRecord()
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    /** Starts mic capture + classification. Callback runs on the main thread. */
    fun start(onResults: (List<Category>, AudioStats) -> Unit) {
        if (running) return
        running = true
        record.startRecording()

        val loop = object : Runnable {
            override fun run() {
                if (!running) return
                tensorAudio.load(record)
                val stats = computeStats(tensorAudio.tensorBuffer.floatArray)
                val results = classifier.classify(tensorAudio)
                val sorted = results[0].categories.sortedByDescending { it.score }
                onResults(sorted, stats)
                handler.postDelayed(this, INTERVAL_MS)
            }
        }
        handler.post(loop)
    }

    /** RMS + peak of the raw samples (floats in [-1, 1]) converted to dBFS. */
    private fun computeStats(samples: FloatArray): AudioStats {
        if (samples.isEmpty()) return AudioStats(SILENCE_DB, SILENCE_DB)
        var sumSq = 0.0
        var peak = 0f
        for (s in samples) {
            sumSq += s * s
            val a = if (s < 0) -s else s
            if (a > peak) peak = a
        }
        val rms = sqrt(sumSq / samples.size).toFloat()
        return AudioStats(toDb(rms), toDb(peak))
    }

    private fun toDb(amplitude: Float): Float =
        if (amplitude > 1e-7f) 20f * log10(amplitude) else SILENCE_DB

    /** Stops everything and releases the mic + model. Call from onDestroy(). */
    fun stop() {
        if (!running) return
        running = false
        handler.removeCallbacksAndMessages(null)
        record.stop()
        classifier.close()
    }
}
