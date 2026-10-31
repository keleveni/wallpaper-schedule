package com.keleveni.wallpaperscheduler.ui.settings

import android.app.Application
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keleveni.wallpaperscheduler.AppContainer
import com.keleveni.wallpaperscheduler.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionStatus(
    val canScheduleExactAlarms: Boolean,
    val ignoringBatteryOptimizations: Boolean,
    val wallpaperChangeAllowed: Boolean,
)

class SettingsViewModel(
    private val application: Application,
    private val container: AppContainer,
) : ViewModel() {
    val autoChangeEnabled: StateFlow<Boolean> = container.settings.autoChangeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _permissions = MutableStateFlow(readPermissions())
    val permissions: StateFlow<PermissionStatus> = _permissions.asStateFlow()

    /** 権限は設定アプリで変更されるため、画面に戻るたびに読み直す。 */
    fun refreshPermissions() {
        _permissions.value = readPermissions()
    }

    fun setAutoChangeEnabled(enabled: Boolean) {
        container.applicationScope.launch {
            container.settings.setAutoChangeEnabled(enabled)
            // 有効にした時点で現在の時間帯の壁紙に合わせる
            container.scheduler.sync(restoreCurrent = enabled)
        }
    }

    private fun readPermissions(): PermissionStatus {
        val powerManager = application.getSystemService(PowerManager::class.java)
        return PermissionStatus(
            canScheduleExactAlarms = container.scheduler.canScheduleExactAlarms(),
            ignoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(application.packageName),
            wallpaperChangeAllowed = container.applier.isWallpaperChangeAllowed,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(this[APPLICATION_KEY] as Application, container) }
        }
    }
}
