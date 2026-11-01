package com.keleveni.wallpaperscheduler.schedule

import com.keleveni.wallpaperscheduler.data.ScheduleEntity
import com.keleveni.wallpaperscheduler.data.WallpaperSource
import com.keleveni.wallpaperscheduler.data.WallpaperTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleCalculatorTest {
    private val tokyo = ZoneId.of("Asia/Tokyo")

    private fun schedule(
        id: Long,
        hour: Int,
        minute: Int,
        wallpaperId: Long = id * 10,
        target: WallpaperTarget = WallpaperTarget.BOTH,
        enabled: Boolean = true,
    ) = ScheduleEntity(id, hour, minute, wallpaperId, target, enabled)

    private fun single(home: Long?, lock: Long?) =
        WallpaperAssignment(home?.let(WallpaperSource::Single), lock?.let(WallpaperSource::Single))

    private fun at(hour: Int, minute: Int, second: Int = 0, zone: ZoneId = tokyo) =
        ZonedDateTime.of(2026, 10, 22, hour, minute, second, 0, zone)

    @Test
    fun nextTrigger_picksEarliestUpcomingToday() {
        val schedules = listOf(schedule(1, 7, 0), schedule(2, 12, 0), schedule(3, 19, 30))
        val next = ScheduleCalculator.nextTrigger(schedules, at(10, 0))!!
        assertEquals(at(12, 0), next.time)
        assertEquals(12 * 60, next.minuteOfDay)
    }

    @Test
    fun nextTrigger_wrapsToTomorrow() {
        val schedules = listOf(schedule(1, 7, 0), schedule(2, 19, 30))
        val next = ScheduleCalculator.nextTrigger(schedules, at(20, 0))!!
        assertEquals(at(7, 0).plusDays(1), next.time)
    }

    @Test
    fun nextTrigger_exactlyAtScheduleTimeMovesToNextOccurrence() {
        val schedules = listOf(schedule(1, 7, 0))
        val next = ScheduleCalculator.nextTrigger(schedules, at(7, 0))!!
        assertEquals(at(7, 0).plusDays(1), next.time)
    }

    @Test
    fun nextTrigger_ignoresDisabledSchedules() {
        val schedules = listOf(schedule(1, 12, 0, enabled = false), schedule(2, 19, 0))
        assertEquals(at(19, 0), ScheduleCalculator.nextTrigger(schedules, at(10, 0))!!.time)
        assertNull(ScheduleCalculator.nextTrigger(listOf(schedule(1, 12, 0, enabled = false)), at(10, 0)))
        assertNull(ScheduleCalculator.nextTrigger(emptyList(), at(10, 0)))
    }

    @Test
    fun nextTrigger_usesLocalTimeOfGivenZone() {
        val schedules = listOf(schedule(1, 7, 0))
        val ny = ZoneId.of("America/New_York")
        val next = ScheduleCalculator.nextTrigger(schedules, at(6, 0, zone = ny))!!
        assertEquals(at(7, 0, zone = ny), next.time)
    }

    @Test
    fun nextOccurrence_dstGapShiftsForward() {
        // 2026-03-08 02:30 は America/New_York には存在しない
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 8, 1, 0, 0, 0, ny)
        val next = ScheduleCalculator.nextOccurrence(2 * 60 + 30, now)
        assertEquals(ZonedDateTime.of(2026, 3, 8, 3, 30, 0, 0, ny), next)
        assertTrue(next.isAfter(now))
    }

    @Test
    fun activeAt_returnsMostRecentScheduleBeforeNow() {
        val schedules = listOf(schedule(1, 7, 0), schedule(2, 12, 0), schedule(3, 19, 0))
        assertEquals(single(20, 20), ScheduleCalculator.activeAt(schedules, 15 * 60))
        assertEquals(single(20, 20), ScheduleCalculator.activeAt(schedules, 12 * 60))
    }

    @Test
    fun activeAt_wrapsToPreviousDay() {
        val schedules = listOf(schedule(1, 7, 0), schedule(2, 19, 0))
        assertEquals(single(20, 20), ScheduleCalculator.activeAt(schedules, 3 * 60))
    }

    @Test
    fun activeAt_resolvesHomeAndLockIndependently() {
        val schedules = listOf(
            schedule(1, 7, 0, wallpaperId = 100, target = WallpaperTarget.HOME),
            schedule(2, 8, 0, wallpaperId = 200, target = WallpaperTarget.LOCK),
            schedule(3, 6, 0, wallpaperId = 300, target = WallpaperTarget.BOTH),
        )
        assertEquals(single(100, 200), ScheduleCalculator.activeAt(schedules, 9 * 60))
        assertEquals(single(100, 300), ScheduleCalculator.activeAt(schedules, 7 * 60 + 30))
    }

    @Test
    fun activeAt_laterRegisteredWinsAtSameTime() {
        val schedules = listOf(
            schedule(1, 7, 0, wallpaperId = 100),
            schedule(2, 7, 0, wallpaperId = 200),
        )
        assertEquals(single(200, 200), ScheduleCalculator.activeAt(schedules, 8 * 60))
    }

    @Test
    fun activeAt_ignoresDisabledAndHandlesEmpty() {
        val schedules = listOf(schedule(1, 7, 0, enabled = false))
        assertTrue(ScheduleCalculator.activeAt(schedules, 8 * 60).isEmpty)
    }

    @Test
    fun dueBetween_onlyIncludesSchedulesInWindow() {
        val schedules = listOf(
            schedule(1, 7, 0, wallpaperId = 100, target = WallpaperTarget.HOME),
            schedule(2, 12, 0, wallpaperId = 200, target = WallpaperTarget.LOCK),
        )
        // 12:00 のアラームが時間通りに実行された場合、ホーム画面は変更しない
        assertEquals(single(null, 200), ScheduleCalculator.dueBetween(schedules, 12 * 60, 12 * 60))
    }

    @Test
    fun dueBetween_catchesUpSchedulesMissedDuringDelay() {
        val schedules = listOf(
            schedule(1, 7, 0, wallpaperId = 100),
            schedule(2, 7, 10, wallpaperId = 200, target = WallpaperTarget.HOME),
        )
        // 7:00 のアラームが 7:15 まで遅延した場合、7:10 の予定も反映する
        assertEquals(single(200, 100), ScheduleCalculator.dueBetween(schedules, 7 * 60, 7 * 60 + 15))
    }

    @Test
    fun dueBetween_windowCanCrossMidnight() {
        val schedules = listOf(schedule(1, 23, 59, wallpaperId = 100), schedule(2, 0, 5, wallpaperId = 200))
        assertEquals(single(200, 200), ScheduleCalculator.dueBetween(schedules, 23 * 60 + 59, 10))
    }

    @Test
    fun activeAt_returnsFolderSourceForFolderSchedules() {
        val schedules = listOf(
            schedule(1, 7, 0, wallpaperId = 100),
            ScheduleEntity(2, 12, 0, wallpaperId = null, target = WallpaperTarget.LOCK, folderId = 5),
        )
        assertEquals(
            WallpaperAssignment(WallpaperSource.Single(100), WallpaperSource.Folder(5)),
            ScheduleCalculator.activeAt(schedules, 13 * 60),
        )
    }
}
