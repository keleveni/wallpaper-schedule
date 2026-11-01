package com.keleveni.wallpaperscheduler.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CropRectTest {
    private val eps = 1e-4f

    private fun assertRect(expected: CropRect, actual: CropRect) {
        assertEquals("left", expected.left, actual.left, eps)
        assertEquals("top", expected.top, actual.top, eps)
        assertEquals("right", expected.right, actual.right, eps)
        assertEquals("bottom", expected.bottom, actual.bottom, eps)
    }

    @Test
    fun fit_landscapeImageUsesFullHeightCentered() {
        // 4000x3000 の写真から 1080x2424 の画面比率で切り出す
        val crop = CropRect.fit(imageAspect = 4000f / 3000f, frameAspect = 1080f / 2424f)
        assertEquals(1f, crop.height, eps)
        assertEquals(0.5f, crop.centerX, eps)
        // ピクセル上の縦横比が画面と一致する
        assertEquals(1080f / 2424f, crop.width * 4000f / (crop.height * 3000f), eps)
    }

    @Test
    fun fit_tallImageUsesFullWidth() {
        val crop = CropRect.fit(imageAspect = 9f / 30f, frameAspect = 9f / 20f)
        assertEquals(1f, crop.width, eps)
        assertEquals(20f / 30f, crop.height, eps)
    }

    @Test
    fun fit_clampsInsideImage() {
        val crop = CropRect.fit(imageAspect = 2f, frameAspect = 0.5f, centerX = 0.99f, centerY = 0.5f)
        assertEquals(1f, crop.right, eps)
        assertEquals(0f, crop.top, eps)
        assertEquals(1f, crop.bottom, eps)
    }

    @Test
    fun fit_limitsZoom() {
        val crop = CropRect.fit(imageAspect = 1f, frameAspect = 1f, width = 0.001f, maxZoom = 8f)
        assertEquals(1f / 8f, crop.width, eps)
    }

    @Test
    fun toPixels_staysInsideImageAndNonEmpty() {
        assertEquals(PixelRect(100, 0, 300, 400), CropRect(0.25f, 0f, 0.75f, 1f).toPixels(400, 400))
        val tiny = CropRect(0.9999f, 0.9999f, 1.0001f, 1.0001f).toPixels(100, 100)
        assertEquals(PixelRect(99, 99, 100, 100), tiny)
    }

    @Test
    fun toRawOrientation_normalIsIdentity() {
        val crop = CropRect(0.1f, 0.2f, 0.4f, 0.9f)
        assertRect(crop, crop.toRawOrientation(1))
        assertRect(crop, crop.toRawOrientation(0))
    }

    @Test
    fun toRawOrientation_rotations() {
        // 表示上の左上の領域
        val crop = CropRect(0f, 0f, 0.5f, 0.25f)
        // 180° 回転: 回転前の画像では右下
        assertRect(CropRect(0.5f, 0.75f, 1f, 1f), crop.toRawOrientation(3))
        // 時計回り 90°（ORIENTATION_ROTATE_90）: 表示上の左上は、回転前の画像の左下
        assertRect(CropRect(0f, 0.5f, 0.25f, 1f), crop.toRawOrientation(6))
        // 反時計回り 90°（ORIENTATION_ROTATE_270）: 表示上の左上は、回転前の画像の右上
        assertRect(CropRect(0.75f, 0f, 1f, 0.5f), crop.toRawOrientation(8))
    }

    @Test
    fun toRawOrientation_flips() {
        val crop = CropRect(0f, 0f, 0.5f, 0.25f)
        assertRect(CropRect(0.5f, 0f, 1f, 0.25f), crop.toRawOrientation(2))
        assertRect(CropRect(0f, 0.75f, 0.5f, 1f), crop.toRawOrientation(4))
        assertRect(CropRect(0f, 0f, 0.25f, 0.5f), crop.toRawOrientation(5))
        assertRect(CropRect(0.75f, 0.5f, 1f, 1f), crop.toRawOrientation(7))
    }
}
