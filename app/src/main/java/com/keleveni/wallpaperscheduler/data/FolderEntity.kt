package com.keleveni.wallpaperscheduler.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 壁紙をまとめるフォルダー。スケジュールでフォルダーを指定すると、実行時にその中からランダムに 1 枚選ぶ。 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** フォルダーと壁紙の所属関係。1 枚の壁紙を複数のフォルダーに入れられる。 */
@Entity(
    tableName = "folder_wallpapers",
    primaryKeys = ["folderId", "wallpaperId"],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WallpaperEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallpaperId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("wallpaperId")],
)
data class FolderWallpaperCrossRef(val folderId: Long, val wallpaperId: Long)

/** フォルダーと、そこに含まれる壁紙の ID。 */
data class Folder(val entity: FolderEntity, val wallpaperIds: Set<Long>) {
    val id: Long get() = entity.id
    val name: String get() = entity.name
}
