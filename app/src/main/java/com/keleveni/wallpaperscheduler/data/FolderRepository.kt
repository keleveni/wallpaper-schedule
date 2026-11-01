package com.keleveni.wallpaperscheduler.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FolderRepository(
    private val dao: FolderDao,
    private val scheduleDao: ScheduleDao,
) {
    val folders: Flow<List<Folder>> = combine(dao.observeAll(), dao.observeMembers()) { folders, members ->
        val byFolder = members.groupBy({ it.folderId }, { it.wallpaperId })
        folders.map { Folder(it, byFolder[it.id].orEmpty().toSet()) }
    }

    suspend fun wallpaperIds(folderId: Long): List<Long> = dao.wallpaperIds(folderId)

    suspend fun create(name: String): Long = dao.insert(FolderEntity(name = name, createdAt = System.currentTimeMillis()))

    suspend fun rename(folderId: Long, name: String) = dao.rename(folderId, name)

    suspend fun add(folderId: Long, wallpaperIds: Collection<Long>) =
        dao.addMembers(wallpaperIds.map { FolderWallpaperCrossRef(folderId, it) })

    /** フォルダーに含める壁紙を [wallpaperIds] に置き換える。 */
    suspend fun setWallpapers(folderId: Long, wallpaperIds: Set<Long>) = dao.setMembers(folderId, wallpaperIds)

    suspend fun countSchedulesUsing(folderId: Long): Int = scheduleDao.countByFolder(folderId)

    /** フォルダーを削除する。壁紙は削除しない。このフォルダーを使うスケジュールは外部キー制約により削除される。 */
    suspend fun delete(folderId: Long) = dao.delete(folderId)
}
