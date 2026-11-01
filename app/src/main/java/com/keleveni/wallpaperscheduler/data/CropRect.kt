package com.keleveni.wallpaperscheduler.data

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 壁紙として表示する範囲。画像の幅・高さに対する割合（0〜1）で表し、
 * 画像は EXIF の回転情報を反映した向きを基準とする。
 */
data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2

    /** 幅 [imageWidth]・高さ [imageHeight] の画像におけるピクセル範囲。画像の内側に収め、空にはしない。 */
    fun toPixels(imageWidth: Int, imageHeight: Int): PixelRect {
        val l = (left * imageWidth).roundToInt().coerceIn(0, imageWidth - 1)
        val t = (top * imageHeight).roundToInt().coerceIn(0, imageHeight - 1)
        val r = (right * imageWidth).roundToInt().coerceIn(l + 1, imageWidth)
        val b = (bottom * imageHeight).roundToInt().coerceIn(t + 1, imageHeight)
        return PixelRect(l, t, r, b)
    }

    /**
     * EXIF の向き [orientation] を反映した画像上の範囲を、ファイルに記録された（回転前の）画像上の範囲に変換する。
     * [orientation] は `ExifInterface.ORIENTATION_*` の値。
     */
    fun toRawOrientation(orientation: Int): CropRect {
        // 表示上の点 (x, y) が、回転前の画像のどの点に当たるか
        val toRaw: (Float, Float) -> Pair<Float, Float> = when (orientation) {
            2 -> { x, y -> (1 - x) to y } // 左右反転
            3 -> { x, y -> (1 - x) to (1 - y) } // 180° 回転
            4 -> { x, y -> x to (1 - y) } // 上下反転
            5 -> { x, y -> y to x } // 転置
            6 -> { x, y -> y to (1 - x) } // 時計回りに 90° 回転
            7 -> { x, y -> (1 - y) to (1 - x) } // 反転転置
            8 -> { x, y -> (1 - y) to x } // 反時計回りに 90° 回転
            else -> { x, y -> x to y }
        }
        val (x1, y1) = toRaw(left, top)
        val (x2, y2) = toRaw(right, bottom)
        return CropRect(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2))
    }

    companion object {
        /**
         * 縦横比 [imageAspect]（幅 / 高さ）の画像から、縦横比 [frameAspect] の範囲を切り出す。
         * 中心を ([centerX], [centerY])、幅を [width] とし、画像の内側に収まるよう調整する。
         *
         * @param maxZoom 画像全体に収まる最大の範囲に対して、どこまで拡大できるか
         */
        fun fit(
            imageAspect: Float,
            frameAspect: Float,
            centerX: Float = 0.5f,
            centerY: Float = 0.5f,
            width: Float = Float.MAX_VALUE,
            maxZoom: Float = 8f,
        ): CropRect {
            // 範囲の高さ（割合）= 幅（割合）× ratio のとき、ピクセル上の縦横比が frameAspect になる
            val ratio = imageAspect / frameAspect
            val maxWidth = min(1f, 1f / ratio)
            val w = width.coerceIn(maxWidth / maxZoom, maxWidth)
            val h = min(w * ratio, 1f)
            val l = (centerX - w / 2).coerceIn(0f, 1f - w)
            val t = (centerY - h / 2).coerceIn(0f, 1f - h)
            return CropRect(l, t, l + w, t + h)
        }
    }
}

data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}
