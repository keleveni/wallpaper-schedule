package com.keleveni.wallpaperscheduler.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val autoChangeKey = booleanPreferencesKey("auto_change_enabled")
    private val lastHomeKey = longPreferencesKey("last_home_wallpaper_id")
    private val lastLockKey = longPreferencesKey("last_lock_wallpaper_id")

    val autoChangeEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[autoChangeKey] ?: true }

    suspend fun isAutoChangeEnabled(): Boolean = autoChangeEnabled.first()

    suspend fun setAutoChangeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[autoChangeKey] = enabled }
    }

    /** 最後に適用した壁紙。フォルダーからランダムに選ぶ際に、同じ壁紙が続かないようにするために使う。 */
    suspend fun lastApplied(): LastApplied {
        val preferences = context.dataStore.data.first()
        return LastApplied(home = preferences[lastHomeKey], lock = preferences[lastLockKey])
    }

    suspend fun recordApplied(wallpaperId: Long, home: Boolean, lock: Boolean) {
        context.dataStore.edit {
            if (home) it[lastHomeKey] = wallpaperId
            if (lock) it[lastLockKey] = wallpaperId
        }
    }
}

data class LastApplied(val home: Long?, val lock: Long?)
