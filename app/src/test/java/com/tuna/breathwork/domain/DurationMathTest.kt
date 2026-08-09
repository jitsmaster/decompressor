package com.tuna.breathwork.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationMathTest {

    @Test
    fun `cycles for exact 2-minute panic preset - sigh 10s cycle`() {
        assertEquals(12, cyclesForDuration(120_000, 10_000))
    }

    @Test
    fun `5 minute preset on 16s box cycle rounds to 19 cycles`() {
        // 300 s / 16 s = 18.75 → 19 cycles = 304 s
        assertEquals(19, cyclesForDuration(300_000, 16_000))
    }

    @Test
    fun `10 minute preset on 19s 478 cycle rounds to 32 cycles`() {
        // 600 s / 19 s = 31.6 → 32 cycles = 608 s
        assertEquals(32, cyclesForDuration(600_000, 19_000))
    }

    @Test
    fun `tiny target clamps to at least one cycle`() {
        assertEquals(1, cyclesForDuration(1, 10_000))
    }
}
