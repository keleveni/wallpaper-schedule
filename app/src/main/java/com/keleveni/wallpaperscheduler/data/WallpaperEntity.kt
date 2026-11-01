package com.keleveni.wallpaperscheduler.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 登録済みの壁紙。画像はアプリの内部ストレージにコピーして保持する。
 *
 * @property fileName `filesDir/wallpapers/` 配下のファイル名
 * @property crop 壁紙として表示する範囲。null の場合は OS に任せる（中央を基準に画面いっぱいに表示される）
 */
@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val createdAt: Long,
    @Embedded(prefix = "crop_") val crop: CropRect? = null,
)
