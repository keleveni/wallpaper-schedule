package com.keleveni.wallpaperscheduler.ui.schedules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keleveni.wallpaperscheduler.R
import com.keleveni.wallpaperscheduler.data.Folder
import com.keleveni.wallpaperscheduler.data.WallpaperEntity
import com.keleveni.wallpaperscheduler.ui.components.FolderCover
import com.keleveni.wallpaperscheduler.ui.components.TargetSelector
import com.keleveni.wallpaperscheduler.ui.components.WallpaperImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditScreen(
    onClose: () -> Unit,
    viewModel: ScheduleEditViewModel = viewModel(factory = ScheduleEditViewModel.Factory),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    val current = form
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val timeState = rememberTimePickerState(
        initialHour = current.hour,
        initialMinute = current.minute,
        is24Hour = true,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (current.isNew) R.string.schedule_edit_title_new else R.string.schedule_edit_title_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!current.isNew) {
                        IconButton(onClick = {
                            viewModel.delete()
                            onClose()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                    TextButton(
                        enabled = current.canSave,
                        onClick = { if (viewModel.save(timeState.hour, timeState.minute)) onClose() },
                    ) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section(stringResource(R.string.schedule_edit_time)) {
                TimePicker(state = timeState, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(
                    stringResource(R.string.schedule_edit_repeat_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Section(stringResource(R.string.schedule_edit_wallpaper)) {
                SourceSelector(
                    useFolder = current.useFolder,
                    onSelect = viewModel::setUseFolder,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                if (current.useFolder) {
                    if (folders.isEmpty()) {
                        Text(
                            stringResource(R.string.schedule_edit_no_folders),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    } else {
                        FolderPicker(
                            folders = folders,
                            wallpapers = wallpapers,
                            selectedId = current.folderId,
                            onSelect = viewModel::selectFolder,
                        )
                        Text(
                            stringResource(R.string.schedule_edit_folder_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                } else if (wallpapers.isEmpty()) {
                    Text(
                        stringResource(R.string.schedule_edit_no_wallpapers),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                } else {
                    WallpaperPicker(
                        wallpapers = wallpapers,
                        selectedId = current.wallpaperId,
                        onSelect = viewModel::selectWallpaper,
                    )
                }
            }

            Section(stringResource(R.string.schedule_edit_target)) {
                TargetSelector(
                    selected = current.target,
                    onSelect = viewModel::selectTarget,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.schedule_edit_enabled),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = current.enabled, onCheckedChange = viewModel::setEnabled)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        content()
    }
}

@Composable
private fun SourceSelector(useFolder: Boolean, onSelect: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(false to R.string.schedule_edit_source_wallpaper, true to R.string.schedule_edit_source_folder)
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == useFolder,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun WallpaperPicker(
    wallpapers: List<WallpaperEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(wallpapers, key = { it.id }) { wallpaper ->
            PickerItem(selected = wallpaper.id == selectedId, onClick = { onSelect(wallpaper.id) }) {
                WallpaperImage(wallpaper, Modifier.matchParentSize())
            }
        }
    }
}

@Composable
private fun FolderPicker(
    folders: List<Folder>,
    wallpapers: List<WallpaperEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(folders, key = { it.id }) { folder ->
            PickerItem(selected = folder.id == selectedId, onClick = { onSelect(folder.id) }) {
                FolderCover(folder, wallpapers, Modifier.matchParentSize())
            }
        }
    }
}

@Composable
private fun PickerItem(selected: Boolean, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(width = 90.dp, height = 160.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), shape) else Modifier,
            )
            .clickable(onClick = onClick),
    ) {
        content()
        if (selected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
        }
    }
}
