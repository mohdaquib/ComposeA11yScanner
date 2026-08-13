package com.composea11yscanner.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SolidBackgroundTextColorEstimatorTest {

    @Test
    fun `solid background and repeated foreground produce a measurement`() {
        val pixels = IntArray(100) { 0xFFFFFFFF.toInt() }
        listOf(11, 13, 15, 22, 24, 26, 31, 35, 42, 44, 46, 55).forEach {
            pixels[it] = 0xFFB0B0B0.toInt()
        }

        val colors = SolidBackgroundTextColorEstimator.estimate(pixels, width = 10)

        assertNotNull(colors)
        assertEquals(0xFFFFFFFF.toInt(), colors?.background?.toArgb())
        assertEquals(0xFFB0B0B0.toInt(), colors?.foreground?.toArgb())
    }

    @Test
    fun `crop without a dominant background is skipped`() {
        val pixels = IntArray(100) { index ->
            val channel = (index * 2).coerceAtMost(255)
            (0xFF shl 24) or (channel shl 16) or (channel shl 8) or channel
        }

        assertNull(SolidBackgroundTextColorEstimator.estimate(pixels, width = 10))
    }

    @Test
    fun `inline code surface is not mistaken for foreground text`() {
        val width = 20
        val pixels = IntArray(width * 10) { 0xFFE1E2EC.toInt() }

        for (y in 3..6) {
            for (x in 5..14) {
                pixels[y * width + x] = 0xFFFAF8FE.toInt()
            }
        }

        listOf(21, 23, 25, 27, 42, 46, 61, 63, 65, 67, 82, 86, 101, 103, 105, 107)
            .forEach { pixels[it] = 0xFF5D5F68.toInt() }

        val colors = SolidBackgroundTextColorEstimator.estimate(pixels, width)

        assertNotNull(colors)
        assertEquals(0xFFE1E2EC.toInt(), colors?.background?.toArgb())
        assertEquals(0xFF5D5F68.toInt(), colors?.foreground?.toArgb())
    }
}
