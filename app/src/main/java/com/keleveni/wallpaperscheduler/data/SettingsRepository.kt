package com.keleveni.wallpaperscheduler.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val autoChangeKey = booleanPreferencesKey("auto_change_enabled")

    val autoChangeEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[autoChangeKey] ?: true }

    suspend fun isAutoChangeEnabled(): Boolean = autoChangeEnabled.first()

    suspend fun setAutoChangeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[autoChangeKey] = enabled }
    }
}
