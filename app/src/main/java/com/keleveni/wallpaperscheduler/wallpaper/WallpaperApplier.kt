package com.keleveni.wallpaperscheduler.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.util.Log
import com.keleveni.wallpaperscheduler.data.WallpaperEntity
import com.keleveni.wallpaperscheduler.data.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** WallpaperManager を使って壁紙を設定する。 */
class WallpaperApplier(
    context: Context,
    private val repository: WallpaperRepository,
) {
    private val manager = WallpaperManager.getInstance(context)

    val isWallpaperChangeAllowed: Boolean
        get() = manager.isWallpaperSupported && manager.isSetWallpaperAllowed

    /**
     * ホーム画面に [home]、ロック画面に [lock] を設定する。null の対象は変更しない。
     * 片方の失敗がもう片方に影響しないよう、対象ごとに独立して設定する。
     *
     * @return 全ての対象への設定に成功した場合 true
     */
    suspend fun apply(home: WallpaperEntity?, lock: WallpaperEntity?): Boolean = withContext(Dispatchers.IO) {
        if (home != null && home.id == lock?.id) {
            return@withContext set(home, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        }
        val homeResult = home?.let { set(it, WallpaperManager.FLAG_SYSTEM) } ?: true
        val lockResult = lock?.let { set(it, WallpaperManager.FLAG_LOCK) } ?: true
        homeResult && lockResult
    }

    private fun set(wallpaper: WallpaperEntity, which: Int): Boolean = try {
        check(isWallpaperChangeAllowed) { "Wallpaper change is not allowed on this device" }
        repository.fileOf(wallpaper).inputStream().use { manager.setStream(it, null, true, which) }
        true
    } catch (e: Exception) {
        Log.w(TAG, "Failed to set wallpaper ${wallpaper.id} (which=$which)", e)
        false
    }

    private companion object {
        const val TAG = "WallpaperApplier"
    }
}
