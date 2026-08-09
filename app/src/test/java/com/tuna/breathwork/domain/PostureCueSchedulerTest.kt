package com.tuna.breathwork.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostureCueSchedulerTest {

    private val templates = listOf("Long spine, soft belly", "Let your shoulders drop", "Unclench your jaw")

    @Test
    fun `no cue in first three cycles`() {
        val scheduler = PostureCueScheduler(templates)
        assertNull(scheduler.cueForCycle(0))
        assertNull(scheduler.cueForCycle(1))
        assertNull(scheduler.cueForCycle(2))
    }

    @Test
    fun `cue on every fourth cycle - one based`() {
        val scheduler = PostureCueScheduler(templates)
        assertEquals("Long spine, soft belly", scheduler.cueForCycle(3))  // 4th breath
        assertEquals("Let your shoulders drop", scheduler.cueForCycle(7)) // 8th breath
        assertEquals("Unclench your jaw", scheduler.cueForCycle(11))      // 12th breath
        assertEquals("Long spine, soft belly", scheduler.cueForCycle(15)) // wraps
    }

    @Test
    fun `calm now mode suppresses all cues`() {
        val scheduler = PostureCueScheduler(templates, calmNow = true)
        for (cycle in 0 until 40) {
            assertNull("cycle $cycle must not cue in calm-now mode", scheduler.cueForCycle(cycle))
        }
    }

    @Test
    fun `templates rotate so phrasing varies`() {
        val scheduler = PostureCueScheduler(templates)
        val cues = (0..23).map { scheduler.cueForCycle(it) }.filterNotNull()
        assertTrue("expected 6 cues across 24 cycles", cues.size == 6)
        assertTrue("cues should vary, got: $cues", cues.distinct().size >= 3)
    }
}
