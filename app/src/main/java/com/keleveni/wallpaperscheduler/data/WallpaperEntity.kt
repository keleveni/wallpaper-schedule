package com.keleveni.wallpaperscheduler.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 登録済みの壁紙。画像はアプリの内部ストレージにコピーして保持する。
 *
 * @property fileName `filesDir/wallpapers/` 配下のファイル名
 */
@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val createdAt: Long,
)
