package com.tuna.breathwork.data

/**
 * Ambient listening tracks (SPEC extension: recorded audio from the web).
 * Downloaded from UCLA Mindful — CC BY-NC-ND 4.0 — personal use only, not for
 * commercial distribution. These are full-session tracks, so they play on the
 * Ambient screen rather than inside the sync engine.
 */
data class AmbientTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val assetPath: String,
    val attribution: String,
    /** Bundled speech/silence envelope (build-time) — drives the breathing glyph. */
    val envelopeAsset: String,
)

object AmbientTracks {
    val all = listOf(
        AmbientTrack(
            id = "ucla_breathing_en",
            title = "Breathing Meditation",
            subtitle = "5 min · English",
            assetPath = "ambient/ucla_breathing_en.mp3",
            attribution = "UCLA Mindful · CC BY-NC-ND 4.0",
            envelopeAsset = "ambient/ucla_breathing_en.env.json",
        ),
        AmbientTrack(
            id = "ucla_breathing_zh",
            title = "呼吸冥想",
            subtitle = "5 分钟 · 普通话",
            assetPath = "ambient/ucla_breathing_zh.mp3",
            attribution = "UCLA Mindful · CC BY-NC-ND 4.0",
            envelopeAsset = "ambient/ucla_breathing_zh.env.json",
        ),
    )
}
