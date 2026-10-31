package com.keleveni.wallpaperscheduler.schedule

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.keleveni.wallpaperscheduler.WallpaperSchedulerApp
import kotlinx.coroutines.launch

/** 指定時刻のアラームを受けて壁紙を変更する。 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val minuteOfDay = intent.getIntExtra(EXTRA_MINUTE_OF_DAY, -1)
        if (minuteOfDay < 0) return
        runAsync(context) { onAlarm(minuteOfDay) }
    }

    companion object {
        const val EXTRA_MINUTE_OF_DAY = "minute_of_day"
    }
}

/** 再起動・アプリ更新・時刻/タイムゾーン変更・アラーム権限の変更時にスケジュールを設定し直す。 */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> runAsync(context) { sync(restoreCurrent = true) }

            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> runAsync(context) { sync() }
        }
    }
}

private fun BroadcastReceiver.runAsync(context: Context, block: suspend WallpaperScheduler.() -> Unit) {
    val container = (context.applicationContext as WallpaperSchedulerApp).container
    val pending = goAsync()
    container.applicationScope.launch {
        try {
            container.scheduler.block()
        } finally {
            pending.finish()
        }
    }
}
