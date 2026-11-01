package com.keleveni.wallpaperscheduler.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class RandomWallpaperPickerTest {
    @Test
    fun pick_avoidsRecentWhenOtherCandidatesExist() {
        repeat(100) { seed ->
            assertNotEquals(2L, RandomWallpaperPicker.pick(listOf(1L, 2L, 3L), recent = listOf(2L), Random(seed)))
        }
    }

    @Test
    fun pick_fallsBackToRecentWhenItIsTheOnlyCandidate() {
        assertEquals(2L, RandomWallpaperPicker.pick(listOf(2L), recent = listOf(2L)))
    }

    @Test
    fun pick_returnsNullForEmptyFolder() {
        assertNull(RandomWallpaperPicker.pick(emptyList(), recent = listOf(1L)))
    }

    @Test
    fun pick_coversAllCandidates() {
        val picked = (0 until 200).mapNotNull { RandomWallpaperPicker.pick(listOf(1L, 2L, 3L), emptyList(), Random(it)) }.toSet()
        assertEquals(setOf(1L, 2L, 3L), picked)
    }
}
