package com.tuna.breathwork.data

import com.tuna.breathwork.domain.PhaseType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A breath phase derived from recorded speech: silence → INHALE, speech → EXHALE.
 * Speech is typically produced on the exhale and pauses are where people inhale,
 * so the glyph follows the actual voice when it is [phaseAt] a position.
 */
data class SpeechPhase(val type: PhaseType, val msUntilChange: Long)

/**
 * Speech/silence envelope over a recording: a per-frame flag array (frameMs apart)
 * built at build time from the audio energy. Pure logic — unit-tested.
 */
class SpeechEnvelope(
    val frameMs: Int,
    private val speechFlags: BooleanArray,
) {
    init {
        require(frameMs > 0) { "frameMs must be > 0" }
        require(speechFlags.isNotEmpty()) { "envelope must not be empty" }
    }

    val durationMs: Long get() = frameMs * speechFlags.size.toLong()

    /**
     * Phase at [positionMs]: the frame it falls in, plus how long until the flag
     * changes (or the track ends). Positions outside the track clamp to the ends.
     */
    fun phaseAt(positionMs: Long): SpeechPhase {
        val clamped = positionMs.coerceIn(0L, durationMs - 1)
        val frame = (clamped / frameMs).toInt().coerceIn(0, speechFlags.size - 1)
        val speaking = speechFlags[frame]
        val type = if (speaking) PhaseType.EXHALE else PhaseType.INHALE

        // Find the index where the flag changes (search from the current frame onward)
        var change = frame
        while (change < speechFlags.size && speechFlags[change] == speaking) change++
        val changeMs = change.toLong() * frameMs
        return SpeechPhase(type, (changeMs - clamped).coerceAtLeast(1))
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        @Serializable
        private data class EnvelopeDto(val frameMs: Int, val speech: List<Int>)

        /** Parses the bundled JSON envelope (frameMs + 0/1 flags). */
        fun fromJson(raw: String): SpeechEnvelope {
            val dto = json.decodeFromString<EnvelopeDto>(raw)
            return SpeechEnvelope(dto.frameMs, BooleanArray(dto.speech.size) { dto.speech[it] == 1 })
        }
    }
}
