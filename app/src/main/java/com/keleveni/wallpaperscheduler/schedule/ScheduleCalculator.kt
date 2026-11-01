package com.keleveni.wallpaperscheduler.schedule

import com.keleveni.wallpaperscheduler.data.ScheduleEntity
import com.keleveni.wallpaperscheduler.data.WallpaperSource
import com.keleveni.wallpaperscheduler.data.WallpaperTarget
import java.time.LocalTime
import java.time.ZonedDateTime

private const val MINUTES_PER_DAY = 24 * 60

/** ホーム画面・ロック画面それぞれに適用すべき壁紙の指定。null は変更しないことを表す。 */
data class WallpaperAssignment(val home: WallpaperSource?, val lock: WallpaperSource?) {
    val isEmpty: Boolean get() = home == null && lock == null
}

/** スケジュールから次回の実行時刻や適用すべき壁紙を求める。Android に依存しない純粋なロジック。 */
object ScheduleCalculator {

    /** 有効なスケジュールのうち、[now] より後で最も早い実行時刻。現地時刻（[now] のタイムゾーン）で計算する。 */
    fun nextTrigger(schedules: List<ScheduleEntity>, now: ZonedDateTime): NextTrigger? =
        schedules.asSequence()
            .filter { it.enabled }
            .map { it.minuteOfDay }
            .distinct()
            .map { NextTrigger(it, nextOccurrence(it, now)) }
            .minByOrNull { it.time }

    /** [minuteOfDay] が次に訪れる日時。夏時間の切り替えで存在しない時刻は直後の有効な時刻になる。 */
    fun nextOccurrence(minuteOfDay: Int, now: ZonedDateTime): ZonedDateTime {
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        val today = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
        return if (today.isAfter(now)) today else ZonedDateTime.of(now.toLocalDate().plusDays(1), time, now.zone)
    }

    /** 時刻 [atMinuteOfDay] 時点で有効な「現在の時間帯」の壁紙。再起動時などの復元に使う。 */
    fun activeAt(schedules: List<ScheduleEntity>, atMinuteOfDay: Int): WallpaperAssignment =
        resolve(schedules, atMinuteOfDay, windowMinutes = MINUTES_PER_DAY - 1)

    /**
     * [fromMinuteOfDay] から [toMinuteOfDay] まで（両端を含む、日付をまたいでもよい）に予定されていた
     * スケジュールのうち、対象ごとに最も新しいものの壁紙。OS による実行遅延の間に過ぎた予定も拾う。
     */
    fun dueBetween(
        schedules: List<ScheduleEntity>,
        fromMinuteOfDay: Int,
        toMinuteOfDay: Int,
    ): WallpaperAssignment =
        resolve(schedules, toMinuteOfDay, windowMinutes = (toMinuteOfDay - fromMinuteOfDay).mod(MINUTES_PER_DAY))

    private fun resolve(schedules: List<ScheduleEntity>, at: Int, windowMinutes: Int): WallpaperAssignment {
        val candidates = schedules.filter { it.enabled && (at - it.minuteOfDay).mod(MINUTES_PER_DAY) <= windowMinutes }
        // 時刻が [at] に近いものを優先し、同時刻なら後から登録したものを優先する
        val order = compareBy<ScheduleEntity> { (at - it.minuteOfDay).mod(MINUTES_PER_DAY) }.thenByDescending { it.id }
        fun pick(covers: (WallpaperTarget) -> Boolean) =
            candidates.filter { covers(it.target) }.minWithOrNull(order)?.source
        return WallpaperAssignment(
            home = pick { it.home },
            lock = pick { it.lock },
        )
    }
}

data class NextTrigger(val minuteOfDay: Int, val time: ZonedDateTime)

fun ZonedDateTime.minuteOfDay(): Int = hour * 60 + minute
