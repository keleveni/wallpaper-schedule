package com.keleveni.wallpaperscheduler.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 毎日 [hour]:[minute] に壁紙を [target] に適用するスケジュール。
 * [wallpaperId] と [folderId] はどちらか一方だけを指定する。[folderId] の場合は実行時にフォルダー内からランダムに選ぶ。
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = WallpaperEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallpaperId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("wallpaperId"), Index("folderId")],
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val wallpaperId: Long?,
    val target: WallpaperTarget,
    val enabled: Boolean = true,
    val folderId: Long? = null,
) {
    init {
        require((wallpaperId == null) != (folderId == null)) { "Exactly one of wallpaperId and folderId must be set" }
    }

    val minuteOfDay: Int get() = hour * 60 + minute

    val source: WallpaperSource
        get() = folderId?.let(WallpaperSource::Folder) ?: WallpaperSource.Single(wallpaperId!!)
}

/** スケジュールで適用する壁紙の指定。 */
sealed interface WallpaperSource {
    data class Single(val wallpaperId: Long) : WallpaperSource

    /** フォルダー内からランダムに選ぶ。 */
    data class Folder(val folderId: Long) : WallpaperSource
}
