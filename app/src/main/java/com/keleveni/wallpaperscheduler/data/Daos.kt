package com.keleveni.wallpaperscheduler.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpapers ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE id = :id")
    suspend fun get(id: Long): WallpaperEntity?

    @Insert
    suspend fun insert(wallpaper: WallpaperEntity): Long

    @Delete
    suspend fun delete(wallpaper: WallpaperEntity)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules ORDER BY hour, minute, id")
    suspend fun getAll(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun get(id: Long): ScheduleEntity?

    @Query("SELECT COUNT(*) FROM schedules WHERE wallpaperId = :wallpaperId")
    suspend fun countByWallpaper(wallpaperId: Long): Int

    @Insert
    suspend fun insert(schedule: ScheduleEntity): Long

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Query("UPDATE schedules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun delete(id: Long)
}
