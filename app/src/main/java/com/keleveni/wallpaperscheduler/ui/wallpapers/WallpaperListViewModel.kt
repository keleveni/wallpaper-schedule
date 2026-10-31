package com.keleveni.wallpaperscheduler.ui.wallpapers

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keleveni.wallpaperscheduler.AppContainer
import com.keleveni.wallpaperscheduler.data.WallpaperEntity
import com.keleveni.wallpaperscheduler.data.WallpaperTarget
import com.keleveni.wallpaperscheduler.ui.container
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WallpaperEvent {
    data object ImportFailed : WallpaperEvent
    data class Applied(val success: Boolean) : WallpaperEvent
}

/** 削除確認中の壁紙と、それを使用しているスケジュールの件数。 */
data class PendingDeletion(val wallpaper: WallpaperEntity, val scheduleCount: Int)

class WallpaperListViewModel(private val container: AppContainer) : ViewModel() {
    val wallpapers: StateFlow<List<WallpaperEntity>?> = container.wallpapers.wallpapers
        .map<List<WallpaperEntity>, List<WallpaperEntity>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _pendingDeletion = MutableStateFlow<PendingDeletion?>(null)
    val pendingDeletion: StateFlow<PendingDeletion?> = _pendingDeletion.asStateFlow()

    private val _applying = MutableStateFlow(false)
    val applying: StateFlow<Boolean> = _applying.asStateFlow()

    private val _events = Channel<WallpaperEvent>(Channel.BUFFERED)
    val events: Flow<WallpaperEvent> = _events.receiveAsFlow()

    fun import(uris: List<Uri>) {
        container.applicationScope.launch {
            var failed = false
            for (uri in uris) {
                try {
                    container.wallpapers.import(uri)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to import $uri", e)
                    failed = true
                }
            }
            if (failed) _events.send(WallpaperEvent.ImportFailed)
        }
    }

    /** 壁紙を今すぐ適用する。スケジュールには影響せず、次回の予定時刻にはスケジュール通り変更される。 */
    fun apply(wallpaper: WallpaperEntity, target: WallpaperTarget) {
        if (_applying.value) return
        _applying.value = true
        container.applicationScope.launch {
            val success = try {
                container.applier.apply(
                    home = wallpaper.takeIf { target.home },
                    lock = wallpaper.takeIf { target.lock },
                )
            } finally {
                _applying.value = false
            }
            _events.send(WallpaperEvent.Applied(success))
        }
    }

    fun requestDelete(wallpaper: WallpaperEntity) {
        viewModelScope.launch {
            _pendingDeletion.value = PendingDeletion(wallpaper, container.wallpapers.countSchedulesUsing(wallpaper))
        }
    }

    fun cancelDelete() {
        _pendingDeletion.value = null
    }

    fun confirmDelete() {
        val pending = _pendingDeletion.value ?: return
        _pendingDeletion.value = null
        container.applicationScope.launch {
            container.wallpapers.delete(pending.wallpaper)
            container.scheduler.sync()
        }
    }

    companion object {
        private const val TAG = "WallpaperListViewModel"

        val Factory = viewModelFactory {
            initializer { WallpaperListViewModel(container) }
        }
    }
}
