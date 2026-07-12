package com.example.soundclassifier.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Camera + object detection using MediaPipe tasks-vision,
 * mirroring how YamNetEngine wraps the microphone: MainActivity only handles UI.
 *
 * Usage:
 *   val engine = ObjectDetectionEngine(context, lifecycleOwner)
 *   engine.start(previewView) { detections, imageHeight, imageWidth -> ... }
 *   engine.stop()
 *
 * Callback runs on the MAIN thread.
 */
class ObjectDetectionEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {

    companion object {
        private const val TAG = "ObjectDetectionEngine"
        private const val MODEL_FILE = "efficientdet-lite0.tflite"
        private const val SCORE_THRESHOLD = 0.5f
        private const val MAX_RESULTS = 3
    }

    private var detector: ObjectDetector? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var bitmapBuffer: Bitmap? = null
    private var onResults: ((List<DetectedObject>, Int, Int) -> Unit)? = null

    /** Starts the camera preview + per-frame detection. */
    fun start(
        previewView: PreviewView,
        onResults: (List<DetectedObject>, Int, Int) -> Unit
    ) {
        this.onResults = onResults

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_FILE).build())
            .setScoreThreshold(SCORE_THRESHOLD)
            .setMaxResults(MAX_RESULTS)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(::onMediaPipeResult)
            .setErrorListener { e -> Log.e(TAG, "Detector error: ${e.message}") }
            .build()

        detector = try {
            ObjectDetector.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load detector: ${e.message}")
            return
        }

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get().also { cameraProvider = it }

            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { a -> a.setAnalyzer(analysisExecutor) { image -> analyze(image) } }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun analyze(image: ImageProxy) {
        val det = detector ?: run { image.close(); return }

        val bitmap = bitmapBuffer ?: Bitmap.createBitmap(
            image.width, image.height, Bitmap.Config.ARGB_8888
        ).also { bitmapBuffer = it }

        val rotation = image.imageInfo.rotationDegrees
        image.use { bitmap.copyPixelsFromBuffer(image.planes[0].buffer) }

        val upright = if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        } else bitmap

        try {
            det.detectAsync(BitmapImageBuilder(upright).build(), SystemClock.uptimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "detectAsync failed: ${e.message}")
        }
    }

    private fun onMediaPipeResult(result: ObjectDetectorResult, input: MPImage) {
        val objects = result.detections().mapNotNull { d ->
            val cat = d.categories().firstOrNull() ?: return@mapNotNull null
            DetectedObject(cat.categoryName(), cat.score(), d.boundingBox())
        }
        ContextCompat.getMainExecutor(context).execute {
            onResults?.invoke(objects, input.height, input.width)
        }
    }

    /** Releases camera + model. Call from onDestroy(). */
    fun stop() {
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
        detector?.close()
        detector = null
        onResults = null
    }
}
