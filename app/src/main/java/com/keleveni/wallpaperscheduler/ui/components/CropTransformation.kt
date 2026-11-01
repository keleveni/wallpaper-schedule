package com.keleveni.wallpaperscheduler.ui.components

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation
import com.keleveni.wallpaperscheduler.data.CropRect

/** 壁紙の表示範囲だけを表示するための Coil の変換。Coil は EXIF の向きを反映してから変換を適用する。 */
class CropTransformation(private val crop: CropRect) : Transformation {
    override val cacheKey: String = "crop(${crop.left},${crop.top},${crop.right},${crop.bottom})"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val r = crop.toPixels(input.width, input.height)
        return Bitmap.createBitmap(input, r.left, r.top, r.width, r.height)
    }
}
