package com.keleveni.wallpaperscheduler.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.keleveni.wallpaperscheduler.data.FolderRepository
import com.keleveni.wallpaperscheduler.data.ScheduleEntity
import com.keleveni.wallpaperscheduler.data.ScheduleRepository
import com.keleveni.wallpaperscheduler.data.SettingsRepository
import com.keleveni.wallpaperscheduler.data.WallpaperEntity
import com.keleveni.wallpaperscheduler.data.WallpaperRepository
import com.keleveni.wallpaperscheduler.data.WallpaperSource
import com.keleveni.wallpaperscheduler.wallpaper.WallpaperApplier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZonedDateTime

/**
 * 次に実行すべきスケジュールの時刻に AlarmManager のアラームを 1 つだけ設定する。
 * 常駐サービスは使わず、アラーム発火時にだけプロセスが起動する。
 */
class WallpaperScheduler(
    private val context: Context,
    private val schedules: ScheduleRepository,
    private val wallpapers: WallpaperRepository,
    private val folders: FolderRepository,
    private val settings: SettingsRepository,
    private val applier: WallpaperApplier,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()

    /** 正確な時刻でアラームを設定できるか。false の場合は OS の都合で遅延しうる。 */
    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * 現在のスケジュールと設定に合わせてアラームを設定し直す。
     *
     * @param restoreCurrent true の場合、現在の時間帯に対応する壁紙も適用する（再起動時・時刻変更時など）
     */
    suspend fun sync(restoreCurrent: Boolean = false) = mutex.withLock {
        val all = schedules.getAll()
        if (!settings.isAutoChangeEnabled()) {
            cancelAlarm()
            return@withLock
        }
        scheduleNext(all)
        if (restoreCurrent) {
            apply(ScheduleCalculator.activeAt(all, ZonedDateTime.now().minuteOfDay()), keepCurrent = true)
        }
    }

    /** アラーム発火時の処理。[triggerMinuteOfDay] から現在までに予定されていた壁紙を適用する。 */
    suspend fun onAlarm(triggerMinuteOfDay: Int) = mutex.withLock {
        val all = schedules.getAll()
        if (!settings.isAutoChangeEnabled()) {
            cancelAlarm()
            return@withLock
        }
        // 壁紙の変更に失敗しても以降のスケジュールに影響しないよう、先に次回のアラームを設定する
        scheduleNext(all)
        val now = ZonedDateTime.now().minuteOfDay()
        apply(ScheduleCalculator.dueBetween(all, triggerMinuteOfDay, now), keepCurrent = false)
    }

    /**
     * @param keepCurrent true の場合、フォルダー指定で現在の壁紙がすでにそのフォルダー内のものなら選び直さない。
     *   再起動や時刻変更のたびに壁紙が変わらないようにするため
     */
    private suspend fun apply(assignment: WallpaperAssignment, keepCurrent: Boolean) {
        if (assignment.isEmpty) return
        val last = settings.lastApplied()
        if (assignment.home == assignment.lock) {
            // 両方に同じ指定の場合は、フォルダーからでも同じ壁紙を両方に設定する
            val wallpaper = resolve(assignment.home!!, listOfNotNull(last.home, last.lock), keepCurrent)
            applier.apply(wallpaper, wallpaper)
        } else {
            applier.apply(
                home = assignment.home?.let { resolve(it, listOfNotNull(last.home), keepCurrent) },
                lock = assignment.lock?.let { resolve(it, listOfNotNull(last.lock), keepCurrent) },
            )
        }
    }

    /** [source] から適用する壁紙を決める。null の場合は変更しない。[current] は現在設定されている壁紙の ID。 */
    private suspend fun resolve(source: WallpaperSource, current: List<Long>, keepCurrent: Boolean): WallpaperEntity? {
        val id = when (source) {
            is WallpaperSource.Single -> source.wallpaperId
            is WallpaperSource.Folder -> {
                val members = folders.wallpaperIds(source.folderId)
                if (keepCurrent && current.isNotEmpty() && members.containsAll(current)) return null
                RandomWallpaperPicker.pick(members, current) ?: return null
            }
        }
        return wallpapers.get(id)
    }

    private fun scheduleNext(all: List<ScheduleEntity>) {
        val next = ScheduleCalculator.nextTrigger(all, ZonedDateTime.now())
        if (next == null) {
            cancelAlarm()
            return
        }
        val triggerAt = next.time.toInstant().toEpochMilli()
        val pendingIntent = alarmIntent(next.minuteOfDay)
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            // 確認直後に権限が取り消された場合
            Log.w(TAG, "Exact alarm not permitted, falling back to inexact alarm", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
        Log.d(TAG, "Next wallpaper change scheduled at ${next.time}")
    }

    private fun cancelAlarm() {
        alarmManager.cancel(alarmIntent(0))
    }

    private fun alarmIntent(minuteOfDay: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_MINUTE_OF_DAY, minuteOfDay)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val TAG = "WallpaperScheduler"
    }
}
