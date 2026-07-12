package com.example.soundclassifier.net

import android.util.Log
import com.example.soundclassifier.vision.DetectedObject
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Fire-and-forget HTTP sender: POSTs detection events as JSON to the AI PC.
 *
 * The AI PC runs ai_pc_link/server.py (Flask, port 5000). Phone and PC must
 * be on the SAME Wi-Fi. Set [PC_BASE_URL] to the PC's IPv4 address
 * (on the PC: ipconfig -> "IPv4 Address").
 */
object EventSender {

    // TODO: change to your AI PC's IP address!
    private const val PC_BASE_URL = "http://10.15.233.167:5000"

    private const val TAG = "EventSender"
    private const val COOLDOWN_MS = 1500L   // don't spam the PC every 500ms frame

    private val executor = Executors.newSingleThreadExecutor()
    private var lastSentAt = 0L
    private var lastLabel: String? = null

    /**
     * Sends an audio emergency event. Throttled: same label is sent at most
     * once per [COOLDOWN_MS]; a NEW label is sent immediately.
     */
    fun sendAudioEvent(
        label: String,
        severity: String,
        score: Float,
        proximity: String,
        proximityRange: String,
        rmsDb: Float
    ) {
        val now = System.currentTimeMillis()
        if (label == lastLabel && now - lastSentAt < COOLDOWN_MS) return
        lastLabel = label
        lastSentAt = now

        val json = JSONObject()
            .put("source", "audio")
            .put("label", label)
            .put("severity", severity)
            .put("score", score)
            .put("proximity", proximity)
            .put("proximity_range", proximityRange)
            .put("rms_db", rmsDb)
            .put("timestamp", now)
            .put("detected_at", formatTime(now))

        executor.execute { post(json.toString()) }
    }

    // Vision has its own throttle so audio and camera don't block each other.
    private var lastVisionSentAt = 0L

    /** Sends the current camera frame's detections (skipped if empty/throttled). */
    fun sendVisionEvent(results: List<DetectedObject>) {
        if (results.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastVisionSentAt < COOLDOWN_MS) return
        lastVisionSentAt = now

        val objects = JSONArray()
        for (d in results) {
            objects.put(
                JSONObject()
                    .put("label", d.label)
                    .put("score", d.score)
                    .put("box", JSONObject()
                        .put("left", d.box.left)
                        .put("top", d.box.top)
                        .put("right", d.box.right)
                        .put("bottom", d.box.bottom))
            )
        }
        if (objects.length() == 0) return

        val json = JSONObject()
            .put("source", "vision")
            .put("label", (0 until objects.length()).joinToString {
                objects.getJSONObject(it).getString("label")
            })
            .put("objects", objects)
            .put("timestamp", now)
            .put("detected_at", formatTime(now))

        executor.execute { post(json.toString()) }
    }

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(millis))

    private fun post(body: String) {
        try {
            val conn = URL("$PC_BASE_URL/event").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            if (code != 200) Log.w(TAG, "PC responded $code")
            conn.disconnect()
        } catch (e: Exception) {
            // Offline / wrong IP — never crash the detection loop over networking.
            Log.w(TAG, "send failed: ${e.message}")
        }
    }
}
