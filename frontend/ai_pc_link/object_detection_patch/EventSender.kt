package org.tensorflow.lite.examples.objectdetection

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.task.vision.detector.Detection
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Fire-and-forget HTTP sender: POSTs object detections as JSON to the AI PC.
 *
 * The AI PC runs ai_pc_link/server.py (Flask, port 5000). Phone and PC must
 * be on the SAME Wi-Fi. Set [PC_BASE_URL] to the PC's IPv4 address
 * (on the PC: ipconfig -> "IPv4 Address").
 */
object EventSender {

    // TODO: change to your AI PC's IP address! (same value as in the audio app)
    private const val PC_BASE_URL = "http://192.168.1.42:5000"

    private const val TAG = "EventSender"
    private const val COOLDOWN_MS = 1000L   // camera runs many FPS — throttle

    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var lastSentAt = 0L

    /** Sends the current frame's detections (skipped if empty or throttled). */
    fun sendDetections(results: List<Detection>?) {
        if (results.isNullOrEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastSentAt < COOLDOWN_MS) return
        lastSentAt = now

        val objects = JSONArray()
        for (d in results) {
            val cat = d.categories.firstOrNull() ?: continue
            objects.put(
                JSONObject()
                    .put("label", cat.label)
                    .put("score", cat.score)
                    .put("box", JSONObject()
                        .put("left", d.boundingBox.left)
                        .put("top", d.boundingBox.top)
                        .put("right", d.boundingBox.right)
                        .put("bottom", d.boundingBox.bottom))
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

        executor.execute { post(json.toString()) }
    }

    private fun post(body: String) {
        try {
            val conn = URL("$PC_BASE_URL/event").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode != 200) Log.w(TAG, "PC responded ${conn.responseCode}")
            conn.disconnect()
        } catch (e: Exception) {
            // Wrong IP / PC offline — never crash the camera loop over networking.
            Log.w(TAG, "send failed: ${e.message}")
        }
    }
}
