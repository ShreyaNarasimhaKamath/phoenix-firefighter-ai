package com.example.soundclassifier.emergency

import org.tensorflow.lite.support.label.Category

/** How dangerous a detected sound is. Order matters: first = most severe. */
enum class Severity { CRITICAL, HIGH, MEDIUM }

/** One emergency detection: what was heard, how bad it is, how confident YamNet was. */
data class EmergencyEvent(
    val label: String,
    val severity: Severity,
    val score: Float
)

/**
 * Milestone 2: decides which of YamNet's 521 sounds count as emergencies.
 *
 * Labels below are EXACT display names from YamNet's AudioSet class map
 * (compared case-insensitively).
 *
 * Noise robustness for cries for help (Milestone 3):
 *  - Background noise splits YamNet's confidence across several scream/shout
 *    labels, so no single label crosses its threshold. We POOL all help-cry
 *    labels into one combined score per frame.
 *  - A brief cry can be masked in individual frames, so we ACCUMULATE evidence
 *    across frames (exponential moving average + a hit counter over a sliding
 *    window). Repeated weak evidence still triggers.
 *  - When the environment itself is loud (noisy flag from the caller), all
 *    thresholds are scaled DOWN, because confidence is naturally suppressed
 *    under noise.
 */
class EmergencySoundFilter {

    companion object {
        // Pooled help-cry score that triggers instantly in one frame.
        private const val HELP_INSTANT = 0.30f
        // EMA of the pooled score that triggers (sustained weak evidence).
        private const val HELP_EMA = 0.18f
        private const val EMA_ALPHA = 0.35f
        // A frame "hits" if the pooled score is at least this...
        private const val HIT_FLOOR = 0.10f
        // ...and this many hits within the window triggers (3 of last 6 frames = 3s).
        private const val HIT_WINDOW = 6
        private const val HITS_NEEDED = 3
        // In noisy environments every threshold is multiplied by this.
        private const val NOISY_SCALE = 0.65f
        // Weight given to "Speech" when the voice is shouted (loud + above floor).
        private const val SHOUTED_SPEECH_WEIGHT = 0.6f
    }

    /** Sounds a person makes when calling for help — pooled together. */
    private val helpCryLabels = setOf(
        "screaming",
        "shout",
        "yell",
        "bellow",
        "battle cry",
        "children shouting",
        "crying, sobbing",
        "wail, moan",
        "whimper",
        "shriek",
        "screech"
    )

    private val emergencyLabels: Map<String, Severity> = buildMap {
        // ---- CRITICAL: immediate danger ----
        put("gunshot, gunfire", Severity.CRITICAL)
        put("machine gun", Severity.CRITICAL)
        put("fusillade", Severity.CRITICAL)
        put("artillery fire", Severity.CRITICAL)
        put("explosion", Severity.CRITICAL)
        put("screaming", Severity.CRITICAL)
        put("fire alarm", Severity.CRITICAL)
        put("smoke detector, smoke alarm", Severity.CRITICAL)
        put("siren", Severity.CRITICAL)
        put("civil defense siren", Severity.CRITICAL)
        put("police car (siren)", Severity.CRITICAL)
        put("ambulance (siren)", Severity.CRITICAL)
        put("fire engine, fire truck (siren)", Severity.CRITICAL)
        put("emergency vehicle", Severity.CRITICAL)

        // ---- HIGH: likely trouble ----
        put("car alarm", Severity.HIGH)
        put("shatter", Severity.HIGH)
        put("glass", Severity.HIGH)
        put("baby cry, infant cry", Severity.HIGH)
        put("crying, sobbing", Severity.HIGH)
        put("shout", Severity.HIGH)
        put("yell", Severity.HIGH)
        put("alarm", Severity.HIGH)

        // ---- MEDIUM: worth attention ----
        put("vehicle horn, car horn, honking", Severity.MEDIUM)
        put("boom", Severity.MEDIUM)
        put("breaking", Severity.MEDIUM)
        put("thump, thud", Severity.MEDIUM)
        put("whimper", Severity.MEDIUM)
        put("children shouting", Severity.MEDIUM)
    }

    /** Lower bar for critical sounds — better to over-warn than miss a gunshot. */
    private val minScore = mapOf(
        Severity.CRITICAL to 0.25f,
        Severity.HIGH to 0.35f,
        Severity.MEDIUM to 0.45f
    )

    // --- Cross-frame state for help-cry accumulation ---
    private var helpEma = 0f
    private val recentHits = ArrayDeque<Boolean>()

    /**
     * Checks one classification frame.
     *
     * @param categories   YamNet results for this frame
     * @param noisy        true when the ambient noise floor is high — thresholds
     *                     are lowered so cries for help aren't missed
     * @param shoutedVoice true when this frame's voice is LOUD and well above
     *                     the noise floor. YamNet labels a yelled "HELP!" as
     *                     plain "Speech" (it hears sound, not words), so
     *                     shouted speech is counted as help-cry evidence.
     * @return the MOST SEVERE emergency found, or null if everything is normal.
     */
    fun check(
        categories: List<Category>,
        noisy: Boolean = false,
        shoutedVoice: Boolean = false
    ): EmergencyEvent? {
        val scale = if (noisy) NOISY_SCALE else 1f

        // ---- 1. Pooled + accumulated help-cry detection ----
        val helpCries = categories.filter { it.label.trim().lowercase() in helpCryLabels }
        var pooled = 0f
        for (c in helpCries) pooled += c.score
        if (shoutedVoice) {
            val speech = categories.firstOrNull { it.label.trim().lowercase() == "speech" }
            if (speech != null) pooled += speech.score * SHOUTED_SPEECH_WEIGHT
        }
        if (pooled > 1f) pooled = 1f

        helpEma = EMA_ALPHA * pooled + (1 - EMA_ALPHA) * helpEma
        recentHits.addLast(pooled >= HIT_FLOOR * scale)
        while (recentHits.size > HIT_WINDOW) recentHits.removeFirst()

        val helpDetected =
            pooled >= HELP_INSTANT * scale ||
            helpEma >= HELP_EMA * scale ||
            recentHits.count { it } >= HITS_NEEDED

        if (helpDetected) {
            val best = helpCries.maxByOrNull { it.score }
            return EmergencyEvent(
                label = best?.label ?: "Cry for help",
                severity = Severity.CRITICAL,
                score = maxOf(pooled, helpEma)
            )
        }

        // ---- 2. Regular per-label check (thresholds scaled down in noise) ----
        return categories
            .mapNotNull { c ->
                val severity = emergencyLabels[c.label.trim().lowercase()] ?: return@mapNotNull null
                if (c.score >= (minScore[severity] ?: 1f) * scale) {
                    EmergencyEvent(c.label, severity, c.score)
                } else null
            }
            .minByOrNull { it.severity.ordinal } // CRITICAL beats HIGH beats MEDIUM
    }
}
