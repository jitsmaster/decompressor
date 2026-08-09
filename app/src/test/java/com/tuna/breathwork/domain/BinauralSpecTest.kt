package com.tuna.breathwork.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BinauralSpecTest {

    @Test
    fun `theta uses 200 and 206 hz carriers producing 6 hz beat`() {
        val spec = BinauralSpec.THETA
        assertEquals(200.0, spec.leftHz, 0.001)
        assertEquals(206.0, spec.rightHz, 0.001)
        assertEquals(6.0, spec.beatHz, 0.001)
    }

    @Test
    fun `alpha uses 200 and 210 hz carriers producing 10 hz beat`() {
        val spec = BinauralSpec.ALPHA
        assertEquals(200.0, spec.leftHz, 0.001)
        assertEquals(210.0, spec.rightHz, 0.001)
        assertEquals(10.0, spec.beatHz, 0.001)
    }
}
