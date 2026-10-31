package com.keleveni.wallpaperscheduler.data

/** 壁紙の変更対象。 */
enum class WallpaperTarget(val home: Boolean, val lock: Boolean) {
    HOME(home = true, lock = false),
    LOCK(home = false, lock = true),
    BOTH(home = true, lock = true),
}
