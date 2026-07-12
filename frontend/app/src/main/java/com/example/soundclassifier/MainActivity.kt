package com.example.soundclassifier

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.soundclassifier.emergency.EmergencySoundFilter
import com.example.soundclassifier.emergency.ProximityEstimator
import com.example.soundclassifier.emergency.Severity
import com.example.soundclassifier.ml.AudioStats
import com.example.soundclassifier.ml.YamNetEngine
import com.example.soundclassifier.net.EventSender
import com.example.soundclassifier.vision.DetectionOverlayView
import com.example.soundclassifier.vision.ObjectDetectionEngine
import androidx.camera.view.PreviewView
import org.tensorflow.lite.support.label.Category

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SoundClassifier"
        private const val PERMISSION_REQUEST_CODE = 1
        private const val ALERT_HOLD_MS = 3000L      // keep banner up 3s after last detection
        private const val VIBRATE_COOLDOWN_MS = 5000L
    }

    private var engine: YamNetEngine? = null
    private var visionEngine: ObjectDetectionEngine? = null
    private val emergencyFilter = EmergencySoundFilter()
    private val proximityEstimator = ProximityEstimator()

    private lateinit var previewView: PreviewView
    private lateinit var detectionOverlay: DetectionOverlayView

    private lateinit var statusText: TextView
    private lateinit var alertCard: LinearLayout
    private lateinit var alertLabel: TextView
    private lateinit var alertMeta: TextView
    private lateinit var soundLabels: List<TextView>
    private lateinit var soundBars: List<ProgressBar>

    private var lastEmergencyAt = 0L
    private var lastVibrateAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        statusText = findViewById(R.id.statusText)
        previewView = findViewById(R.id.previewView)
        detectionOverlay = findViewById(R.id.detectionOverlay)
        alertCard = findViewById(R.id.alertCard)
        alertLabel = findViewById(R.id.alertLabel)
        alertMeta = findViewById(R.id.alertMeta)
        soundLabels = listOf(
            findViewById(R.id.sound1Label),
            findViewById(R.id.sound2Label),
            findViewById(R.id.sound3Label)
        )
        soundBars = listOf(
            findViewById(R.id.sound1Bar),
            findViewById(R.id.sound2Bar),
            findViewById(R.id.sound3Bar)
        )

        val needed = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        if (needed.isEmpty()) {
            startMonitoring()
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST_CODE) return

        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
            startMonitoring()
        } else {
            statusText.text = "● Microphone permission required"
            statusText.setTextColor(ContextCompat.getColor(this, R.color.severity_critical))
        }
        if (hasPermission(Manifest.permission.CAMERA)) {
            startCamera()
        }
    }

    private fun hasPermission(permission: String) =
        ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun startMonitoring() {
        statusText.text = "● Listening"
        engine = YamNetEngine(this).also { it.start(::onResults) }
    }

    private fun startCamera() {
        visionEngine = ObjectDetectionEngine(this, this).also { eng ->
            eng.start(previewView) { detections, imageHeight, imageWidth ->
                detectionOverlay.setResults(detections, imageHeight, imageWidth)
                // Stream object detections to the AI PC (fire-and-forget, throttled)
                EventSender.sendVisionEvent(detections)
            }
        }
    }

    /** Called every 500ms with YamNet's results, sorted by confidence. */
    private fun onResults(categories: List<Category>, stats: AudioStats) {
        // --- Live sounds card: top 3 ---
        val top = categories.take(3)
        for (i in soundLabels.indices) {
            val c = top.getOrNull(i)
            if (c != null && c.score > 0.1f) {
                soundLabels[i].text = "${c.label}   ${(c.score * 100).toInt()}%"
                soundBars[i].progress = (c.score * 100).toInt()
            } else {
                soundLabels[i].text = "—"
                soundBars[i].progress = 0
            }
        }

        // --- Emergency check (thresholds adapt when the environment is noisy) ---
        // A yelled "HELP!" reaches YamNet as plain "Speech", so flag frames where
        // the voice is loud AND stands well above the ambient noise floor.
        val snrDb = stats.rmsDb - proximityEstimator.noiseFloorDb
        val shoutedVoice = stats.rmsDb > -20f && snrDb > 15f

        Log.d(TAG, "top=" + categories.take(5).joinToString {
            "${it.label}:${"%.2f".format(it.score)}"
        } + "  rms=${"%.1f".format(stats.rmsDb)}dB floor=${"%.1f".format(proximityEstimator.noiseFloorDb)}dB shouted=$shoutedVoice")

        val event = emergencyFilter.check(
            categories,
            noisy = proximityEstimator.isNoisy,
            shoutedVoice = shoutedVoice
        )
        proximityEstimator.updateNoiseFloor(stats.rmsDb, isEmergencyFrame = event != null)
        val now = System.currentTimeMillis()

        statusText.text = if (proximityEstimator.isNoisy) "● Listening (noisy environment)" else "● Listening"

        if (event != null) {
            lastEmergencyAt = now
            alertCard.visibility = View.VISIBLE
            alertLabel.text = event.label
            val proximity = proximityEstimator.estimate(stats.rmsDb)
            alertMeta.text = "${event.severity}  •  ${(event.score * 100).toInt()}% confidence" +
                "\n${proximity.display} (${proximity.range})"

            // Stream the detection to the AI PC (fire-and-forget, throttled)
            EventSender.sendAudioEvent(
                label = event.label,
                severity = event.severity.name,
                score = event.score,
                proximity = proximity.display,
                proximityRange = proximity.range,
                rmsDb = stats.rmsDb
            )

            val color = when (event.severity) {
                Severity.CRITICAL -> R.color.severity_critical
                Severity.HIGH -> R.color.severity_high
                Severity.MEDIUM -> R.color.severity_medium
            }
            alertCard.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, color))

            if (event.severity == Severity.CRITICAL && now - lastVibrateAt > VIBRATE_COOLDOWN_MS) {
                lastVibrateAt = now
                vibrate()
            }
        } else if (now - lastEmergencyAt > ALERT_HOLD_MS) {
            alertCard.visibility = View.GONE
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(600)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine?.stop()
        engine = null
        visionEngine?.stop()
        visionEngine = null
    }
}
