package com.keleveni.wallpaperscheduler.ui.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keleveni.wallpaperscheduler.AppContainer
import com.keleveni.wallpaperscheduler.data.Folder
import com.keleveni.wallpaperscheduler.data.ScheduleEntity
import com.keleveni.wallpaperscheduler.data.WallpaperEntity
import com.keleveni.wallpaperscheduler.schedule.ScheduleCalculator
import com.keleveni.wallpaperscheduler.ui.container
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** [folder] はフォルダー指定のスケジュールの場合のみ。[wallpapers] は表紙の表示に使う。 */
data class ScheduleItem(
    val schedule: ScheduleEntity,
    val wallpaper: WallpaperEntity?,
    val folder: Folder?,
    val wallpapers: List<WallpaperEntity>,
)

data class ScheduleListUiState(
    val items: List<ScheduleItem> = emptyList(),
    val hasWallpapers: Boolean = false,
    val nextChange: ZonedDateTime? = null,
    val autoChangeEnabled: Boolean = true,
    val canScheduleExactAlarms: Boolean = true,
    val loaded: Boolean = false,
)

class ScheduleListViewModel(private val container: AppContainer) : ViewModel() {
    private val canScheduleExact = MutableStateFlow(container.scheduler.canScheduleExactAlarms())

    // 次回の変更予定の表示を時刻の経過に追従させる
    private val clock = flow {
        while (true) {
            emit(ZonedDateTime.now())
            delay(30_000)
        }
    }

    private val items = combine(
        container.schedules.schedules,
        container.wallpapers.wallpapers,
        container.folders.folders,
    ) { schedules, wallpapers, folders ->
        val wallpaperById = wallpapers.associateBy { it.id }
        val folderById = folders.associateBy { it.id }
        val items = schedules.map {
            ScheduleItem(it, wallpaperById[it.wallpaperId], folderById[it.folderId], wallpapers)
        }
        Triple(schedules, items, wallpapers.isNotEmpty())
    }

    val uiState: StateFlow<ScheduleListUiState> = combine(
        items,
        container.settings.autoChangeEnabled,
        canScheduleExact,
        clock,
    ) { (schedules, items, hasWallpapers), autoEnabled, canExact, now ->
        ScheduleListUiState(
            items = items,
            hasWallpapers = hasWallpapers,
            nextChange = if (autoEnabled) ScheduleCalculator.nextTrigger(schedules, now)?.time else null,
            autoChangeEnabled = autoEnabled,
            canScheduleExactAlarms = canExact,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleListUiState())

    fun refreshPermissions() {
        canScheduleExact.value = container.scheduler.canScheduleExactAlarms()
    }

    fun setEnabled(schedule: ScheduleEntity, enabled: Boolean) {
        container.applicationScope.launch {
            container.schedules.setEnabled(schedule.id, enabled)
            container.scheduler.sync()
        }
    }

    fun delete(schedule: ScheduleEntity) {
        container.applicationScope.launch {
            container.schedules.delete(schedule.id)
            container.scheduler.sync()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ScheduleListViewModel(container) }
        }
    }
}
