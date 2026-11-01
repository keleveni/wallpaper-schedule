package com.keleveni.wallpaperscheduler.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Update
    suspend fun update(wallpaper: WallpaperEntity)

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

    @Query("SELECT COUNT(*) FROM schedules WHERE folderId = :folderId")
    suspend fun countByFolder(folderId: Long): Int

    @Insert
    suspend fun insert(schedule: ScheduleEntity): Long

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Query("UPDATE schedules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
abstract class FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt, id")
    abstract fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folder_wallpapers")
    abstract fun observeMembers(): Flow<List<FolderWallpaperCrossRef>>

    @Query("SELECT wallpaperId FROM folder_wallpapers WHERE folderId = :folderId")
    abstract suspend fun wallpaperIds(folderId: Long): List<Long>

    @Insert
    abstract suspend fun insert(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM folders WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addMembers(members: List<FolderWallpaperCrossRef>)

    @Query("DELETE FROM folder_wallpapers WHERE folderId = :folderId AND wallpaperId IN (:wallpaperIds)")
    abstract suspend fun removeMembers(folderId: Long, wallpaperIds: List<Long>)

    @Transaction
    open suspend fun setMembers(folderId: Long, wallpaperIds: Set<Long>) {
        val current = wallpaperIds(folderId).toSet()
        removeMembers(folderId, (current - wallpaperIds).toList())
        addMembers((wallpaperIds - current).map { FolderWallpaperCrossRef(folderId, it) })
    }
}
