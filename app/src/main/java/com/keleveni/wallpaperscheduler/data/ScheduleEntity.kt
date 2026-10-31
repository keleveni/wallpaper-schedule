package com.keleveni.wallpaperscheduler.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 毎日 [hour]:[minute] に [wallpaperId] の壁紙を [target] に適用するスケジュール。 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = WallpaperEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallpaperId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("wallpaperId")],
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val wallpaperId: Long,
    val target: WallpaperTarget,
    val enabled: Boolean = true,
) {
    val minuteOfDay: Int get() = hour * 60 + minute
}
