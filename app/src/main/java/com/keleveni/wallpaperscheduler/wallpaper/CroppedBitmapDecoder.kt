package com.keleveni.wallpaperscheduler.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.keleveni.wallpaperscheduler.data.CropRect
import java.io.File
import java.io.IOException

/** 画像ファイルから [CropRect] の範囲を、EXIF の向きを反映して切り出す。 */
internal object CroppedBitmapDecoder {

    /** 切り出した画像の長辺がこの大きさを下回らない範囲で縮小して読み込み、メモリ使用量を抑える。 */
    private const val TARGET_LONG_SIDE = 2560

    fun decode(file: File, crop: CropRect): Bitmap {
        val orientation = readOrientation(file)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Cannot decode ${file.name}")

        val region = crop.toRawOrientation(orientation).toPixels(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(maxOf(region.width, region.height), TARGET_LONG_SIDE)
        }
        val rect = Rect(region.left, region.top, region.right, region.bottom)
        val cropped = decodeRegion(file, rect, options) ?: decodeFullAndCrop(file, rect, options)
        return cropped.applyOrientation(orientation)
    }

    private fun readOrientation(file: File): Int = try {
        ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun decodeRegion(file: File, rect: Rect, options: BitmapFactory.Options): Bitmap? = try {
        val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(file.path)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(file.path, false)
        }
        try {
            decoder?.decodeRegion(rect, options)
        } finally {
            decoder?.recycle()
        }
    } catch (e: IOException) {
        // 部分読み込みに対応していない形式
        null
    }

    private fun decodeFullAndCrop(file: File, rect: Rect, options: BitmapFactory.Options): Bitmap {
        val full = BitmapFactory.decodeFile(file.path, options) ?: throw IOException("Cannot decode ${file.name}")
        val s = options.inSampleSize
        val left = (rect.left / s).coerceIn(0, full.width - 1)
        val top = (rect.top / s).coerceIn(0, full.height - 1)
        val width = (rect.width() / s).coerceIn(1, full.width - left)
        val height = (rect.height() / s).coerceIn(1, full.height - top)
        return Bitmap.createBitmap(full, left, top, width, height)
    }

    private fun Bitmap.applyOrientation(orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply { postRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(-90f)
            else -> return this
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}

/** 長辺 [longSide] の画像を、[target] を下回らない範囲で縮小する際の inSampleSize（2 の累乗）。 */
internal fun sampleSize(longSide: Int, target: Int): Int {
    var size = 1
    while (longSide / (size * 2) >= target) size *= 2
    return size
}
