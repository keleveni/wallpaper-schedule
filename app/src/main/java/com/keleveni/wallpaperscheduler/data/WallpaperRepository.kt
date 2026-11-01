package com.keleveni.wallpaperscheduler.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** 壁紙画像の取り込み・削除を担う。画像は外部へ送信せず、内部ストレージにのみ保存する。 */
class WallpaperRepository(
    private val context: Context,
    private val dao: WallpaperDao,
    private val scheduleDao: ScheduleDao,
) {
    private val directory: File
        get() = File(context.filesDir, "wallpapers").apply { mkdirs() }

    val wallpapers: Flow<List<WallpaperEntity>> = dao.observeAll()

    fun fileOf(wallpaper: WallpaperEntity): File = File(directory, wallpaper.fileName)

    suspend fun get(id: Long): WallpaperEntity? = dao.get(id)

    /** 端末内の画像 [uri] を内部ストレージへコピーして登録する。 */
    suspend fun import(uri: Uri): WallpaperEntity = withContext(Dispatchers.IO) {
        val file = File(directory, "${UUID.randomUUID()}.img")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Cannot open $uri")
            input.use { src -> file.outputStream().use { src.copyTo(it) } }
            val entity = WallpaperEntity(fileName = file.name, createdAt = System.currentTimeMillis())
            entity.copy(id = dao.insert(entity))
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    /** 壁紙として表示する範囲を保存する。null で OS の既定（中央基準）に戻す。 */
    suspend fun setCrop(wallpaper: WallpaperEntity, crop: CropRect?) {
        dao.update(wallpaper.copy(crop = crop))
    }

    suspend fun countSchedulesUsing(wallpaper: WallpaperEntity): Int =
        scheduleDao.countByWallpaper(wallpaper.id)

    /** 壁紙を削除する。この壁紙を使うスケジュールも外部キー制約により削除される。 */
    suspend fun delete(wallpaper: WallpaperEntity) = withContext(Dispatchers.IO) {
        dao.delete(wallpaper)
        fileOf(wallpaper).delete()
    }
}
