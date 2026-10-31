package com.keleveni.wallpaperscheduler

import android.app.Application
import android.content.Context
import com.keleveni.wallpaperscheduler.data.AppDatabase
import com.keleveni.wallpaperscheduler.data.ScheduleRepository
import com.keleveni.wallpaperscheduler.data.SettingsRepository
import com.keleveni.wallpaperscheduler.data.WallpaperRepository
import com.keleveni.wallpaperscheduler.schedule.WallpaperScheduler
import com.keleveni.wallpaperscheduler.wallpaper.WallpaperApplier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class WallpaperSchedulerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** アプリ全体で共有する依存関係。 */
class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = AppDatabase.create(context)
    val settings = SettingsRepository(context)
    val schedules = ScheduleRepository(database.scheduleDao())
    val wallpapers = WallpaperRepository(context, database.wallpaperDao(), database.scheduleDao())
    val applier = WallpaperApplier(context, wallpapers)
    val scheduler = WallpaperScheduler(context, schedules, wallpapers, settings, applier)
}
