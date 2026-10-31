package com.keleveni.wallpaperscheduler.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [WallpaperEntity::class, ScheduleEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "wallpaper_scheduler.db").build()
    }
}

class Converters {
    @TypeConverter
    fun fromTarget(target: WallpaperTarget): String = target.name

    @TypeConverter
    fun toTarget(value: String): WallpaperTarget = WallpaperTarget.valueOf(value)
}
