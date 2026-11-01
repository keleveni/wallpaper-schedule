package com.keleveni.wallpaperscheduler.ui.schedules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keleveni.wallpaperscheduler.AppContainer
import com.keleveni.wallpaperscheduler.data.Folder
import com.keleveni.wallpaperscheduler.data.ScheduleEntity
import com.keleveni.wallpaperscheduler.data.WallpaperEntity
import com.keleveni.wallpaperscheduler.data.WallpaperTarget
import com.keleveni.wallpaperscheduler.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

data class ScheduleForm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val wallpaperId: Long?,
    val folderId: Long?,
    /** true の場合はフォルダーからランダムに選ぶ。 */
    val useFolder: Boolean,
    val target: WallpaperTarget,
    val enabled: Boolean,
) {
    val isNew: Boolean get() = id == 0L
    val canSave: Boolean get() = if (useFolder) folderId != null else wallpaperId != null
}

class ScheduleEditViewModel(
    private val container: AppContainer,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val scheduleId: Long = savedStateHandle[ARG_SCHEDULE_ID] ?: NEW_SCHEDULE_ID

    val wallpapers: StateFlow<List<WallpaperEntity>> = container.wallpapers.wallpapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<Folder>> = container.folders.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow<ScheduleForm?>(null)

    /** 読み込み完了までは null。 */
    val form: StateFlow<ScheduleForm?> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = if (scheduleId == NEW_SCHEDULE_ID) null else container.schedules.get(scheduleId)
            _form.value = if (existing != null) {
                ScheduleForm(
                    id = existing.id,
                    hour = existing.hour,
                    minute = existing.minute,
                    wallpaperId = existing.wallpaperId,
                    folderId = existing.folderId,
                    useFolder = existing.folderId != null,
                    target = existing.target,
                    enabled = existing.enabled,
                )
            } else {
                ScheduleForm(
                    id = 0,
                    hour = LocalTime.now().hour,
                    minute = 0,
                    wallpaperId = container.wallpapers.wallpapers.first().firstOrNull()?.id,
                    folderId = container.folders.folders.first().firstOrNull()?.id,
                    useFolder = false,
                    target = WallpaperTarget.BOTH,
                    enabled = true,
                )
            }
        }
    }

    fun selectWallpaper(id: Long) = _form.update { it?.copy(wallpaperId = id) }

    fun selectFolder(id: Long) = _form.update { it?.copy(folderId = id) }

    fun setUseFolder(useFolder: Boolean) = _form.update { it?.copy(useFolder = useFolder) }

    fun selectTarget(target: WallpaperTarget) = _form.update { it?.copy(target = target) }

    fun setEnabled(enabled: Boolean) = _form.update { it?.copy(enabled = enabled) }

    /** 保存できた場合 true。画面を閉じても処理が中断されないようアプリのスコープで実行する。 */
    fun save(hour: Int, minute: Int): Boolean {
        val form = _form.value ?: return false
        if (!form.canSave) return false
        val schedule = ScheduleEntity(
            id = form.id,
            hour = hour,
            minute = minute,
            wallpaperId = form.wallpaperId.takeUnless { form.useFolder },
            folderId = form.folderId.takeIf { form.useFolder },
            target = form.target,
            enabled = form.enabled,
        )
        container.applicationScope.launch {
            container.schedules.save(schedule)
            container.scheduler.sync()
        }
        return true
    }

    fun delete() {
        val form = _form.value ?: return
        if (form.isNew) return
        container.applicationScope.launch {
            container.schedules.delete(form.id)
            container.scheduler.sync()
        }
    }

    companion object {
        const val ARG_SCHEDULE_ID = "scheduleId"
        const val NEW_SCHEDULE_ID = -1L

        val Factory = viewModelFactory {
            initializer { ScheduleEditViewModel(container, createSavedStateHandle()) }
        }
    }
}
