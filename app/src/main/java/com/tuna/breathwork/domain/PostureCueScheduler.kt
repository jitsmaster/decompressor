package com.tuna.breathwork.domain

/**
 * Rotates posture micro-prompts into the session on every 4th cycle so phrasing
 * stays varied and non-robotic. Fully suppressed in Calm Now mode (SPEC D9).
 */
class PostureCueScheduler(
    private val templates: List<String>,
    private val calmNow: Boolean = false,
) {
    init {
        require(templates.isNotEmpty()) { "templates must not be empty" }
    }

    /** @param cycleIndex zero-based cycle number */
    fun cueForCycle(cycleIndex: Int): String? {
        if (calmNow) return null
        if (cycleIndex < 3) return null // 1st–3rd breath: stay quiet
        if ((cycleIndex + 1) % 4 != 0) return null // every 4th breath (1-based)
        val rotation = cycleIndex / 4
        return templates[rotation % templates.size]
    }
}
