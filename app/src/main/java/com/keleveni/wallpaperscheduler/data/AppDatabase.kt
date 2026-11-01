package com.keleveni.wallpaperscheduler.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [WallpaperEntity::class, ScheduleEntity::class, FolderEntity::class, FolderWallpaperCrossRef::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // 壁紙の表示範囲（crop_*）を追加
        AutoMigration(from = 2, to = 3), // フォルダーを追加し、スケジュールでフォルダーを指定できるようにする
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun folderDao(): FolderDao

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
