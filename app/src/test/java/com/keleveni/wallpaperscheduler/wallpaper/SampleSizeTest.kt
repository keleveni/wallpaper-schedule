package com.keleveni.wallpaperscheduler.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleSizeTest {
    @Test
    fun keepsLongSideAtLeastTarget() {
        assertEquals(1, sampleSize(longSide = 3000, target = 2560))
        assertEquals(1, sampleSize(longSide = 5119, target = 2560))
        assertEquals(2, sampleSize(longSide = 5120, target = 2560))
        assertEquals(4, sampleSize(longSide = 12000, target = 2560))
        assertEquals(1, sampleSize(longSide = 100, target = 2560))
    }
}
