package com.keleveni.wallpaperscheduler.schedule

import kotlin.random.Random

object RandomWallpaperPicker {
    /** [candidates] からランダムに 1 つ選ぶ。[recent] に含まれるものは、他に候補がある限り選ばない。候補が空なら null。 */
    fun pick(candidates: Collection<Long>, recent: Collection<Long>, random: Random = Random.Default): Long? {
        val fresh = candidates.filter { it !in recent }
        return fresh.ifEmpty { candidates.toList() }.randomOrNull(random)
    }
}
