package com.example.soundclassifier.emergency

/** How far away the detected voice/sound likely is. */
enum class Proximity(val display: String, val range: String) {
    VERY_CLOSE("Very close", "< 2 m"),
    NEARBY("Nearby", "2–5 m"),
    IN_AREA("In the area", "5–10 m"),
    FAR("Far away", "> 10 m")
}

/**
 * Estimates how close a detected voice is, from its loudness.
 *
 * Sound pressure falls off with distance, so a louder cry is (roughly) a
 * closer cry. Two signals are combined:
 *  - the frame's absolute level in dBFS, and
 *  - how far the frame stands ABOVE the ambient noise floor (SNR), which we
 *    track with a slow moving average of quiet frames. This keeps estimates
 *    sensible in loud environments where everything is loud.
 *
 * Also exposes [isNoisy] so detection thresholds can adapt to loud settings.
 */
class ProximityEstimator {

    companion object {
        private const val FLOOR_ALPHA_UP = 0.02f    // floor rises slowly
        private const val FLOOR_ALPHA_DOWN = 0.10f  // floor drops faster
        private const val NOISY_FLOOR_DB = -32f     // above this = noisy place
    }

    /** Slow-moving estimate of ambient loudness (dBFS). */
    var noiseFloorDb = -55f
        private set

    /** True when the environment itself is loud (traffic, crowd, machinery). */
    val isNoisy: Boolean get() = noiseFloorDb > NOISY_FLOOR_DB

    /**
     * Feed every frame here. Frames that are part of an emergency event are
     * excluded so a scream doesn't inflate the "ambient" floor.
     */
    fun updateNoiseFloor(rmsDb: Float, isEmergencyFrame: Boolean) {
        if (isEmergencyFrame) return
        val alpha = if (rmsDb > noiseFloorDb) FLOOR_ALPHA_UP else FLOOR_ALPHA_DOWN
        noiseFloorDb += alpha * (rmsDb - noiseFloorDb)
    }

    /** Estimate proximity of the sound in a frame with the given level. */
    fun estimate(rmsDb: Float): Proximity {
        val snrDb = rmsDb - noiseFloorDb

        // Absolute loudness score
        val absScore = when {
            rmsDb > -15f -> 3
            rmsDb > -27f -> 2
            rmsDb > -38f -> 1
            else -> 0
        }
        // How much it stands out over the background
        val snrScore = when {
            snrDb > 25f -> 3
            snrDb > 15f -> 2
            snrDb > 6f -> 1
            else -> 0
        }

        // Weight absolute level slightly higher; round to nearest bucket.
        return when ((absScore * 2 + snrScore + 1) / 3) {
            3 -> Proximity.VERY_CLOSE
            2 -> Proximity.NEARBY
            1 -> Proximity.IN_AREA
            else -> Proximity.FAR
        }
    }
}
