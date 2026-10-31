package com.keleveni.wallpaperscheduler.ui.schedules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keleveni.wallpaperscheduler.R
import com.keleveni.wallpaperscheduler.data.ScheduleEntity
import com.keleveni.wallpaperscheduler.ui.components.WallpaperImage
import com.keleveni.wallpaperscheduler.ui.components.formatTime
import com.keleveni.wallpaperscheduler.ui.components.label
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    onAddSchedule: () -> Unit,
    onEditSchedule: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ScheduleListViewModel = viewModel(factory = ScheduleListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<ScheduleEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val needWallpaperMessage = stringResource(R.string.schedules_add_need_wallpaper)

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.schedules_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (state.hasWallpapers) {
                    onAddSchedule()
                } else {
                    scope.launch { snackbarHostState.showSnackbar(needWallpaperMessage) }
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { NextChangeCard(state.nextChange) }
            if (!state.autoChangeEnabled) {
                item { NoticeCard(Icons.Default.Info, stringResource(R.string.schedules_auto_disabled), onOpenSettings) }
            } else if (!state.canScheduleExactAlarms) {
                item { NoticeCard(Icons.Default.Warning, stringResource(R.string.schedules_exact_alarm_missing), onOpenSettings) }
            }
            if (state.loaded && state.items.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.schedules_empty),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    )
                }
            }
            items(state.items, key = { it.schedule.id }) { item ->
                ScheduleRow(
                    item = item,
                    onClick = { onEditSchedule(item.schedule.id) },
                    onToggle = { viewModel.setEnabled(item.schedule, it) },
                    onDelete = { deleting = item.schedule },
                )
            }
        }
    }

    deleting?.let { schedule ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.schedules_delete_title)) },
            text = {
                Text(stringResource(R.string.schedules_delete_message, formatTime(schedule.hour, schedule.minute)))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(schedule)
                    deleting = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun NextChangeCard(next: ZonedDateTime?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.schedules_next_change), style = MaterialTheme.typography.labelMedium)
                val text = if (next == null) {
                    stringResource(R.string.schedules_next_none)
                } else {
                    val day = if (next.toLocalDate() == LocalDate.now(next.zone)) R.string.day_today else R.string.day_tomorrow
                    stringResource(R.string.schedules_next_format, stringResource(day), formatTime(next.hour, next.minute))
                }
                Text(text, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun NoticeCard(icon: ImageVector, message: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ScheduleRow(
    item: ScheduleItem,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val schedule = item.schedule
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            WallpaperImage(
                wallpaper = item.wallpaper,
                modifier = Modifier.size(width = 48.dp, height = 80.dp).clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                val contentColor = if (schedule.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
                Text(
                    formatTime(schedule.hour, schedule.minute),
                    style = MaterialTheme.typography.displaySmall,
                    color = contentColor,
                )
                Text(
                    stringResource(schedule.target.label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
            }
            Switch(checked = schedule.enabled, onCheckedChange = onToggle)
        }
    }
}
