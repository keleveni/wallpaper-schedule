package com.keleveni.wallpaperscheduler.ui.wallpapers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keleveni.wallpaperscheduler.R
import com.keleveni.wallpaperscheduler.data.WallpaperTarget
import com.keleveni.wallpaperscheduler.ui.components.TargetSelector
import com.keleveni.wallpaperscheduler.ui.components.WallpaperImage

/** 壁紙の全画面プレビュー。ここから手動での適用と削除を行う。 */
@Composable
fun WallpaperPreviewScreen(
    wallpaperId: Long,
    onClose: () -> Unit,
    onAdjustCrop: () -> Unit,
    viewModel: WallpaperListViewModel = viewModel(factory = WallpaperListViewModel.Factory),
) {
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val pendingDeletion by viewModel.pendingDeletion.collectAsStateWithLifecycle()
    val applying by viewModel.applying.collectAsStateWithLifecycle()
    // 壁紙を適用するとダイナミックカラーの更新で Activity が再生成されるため、保存可能な状態で持つ
    var target by rememberSaveable { mutableStateOf(WallpaperTarget.BOTH) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val wallpaper = wallpapers?.find { it.id == wallpaperId }
    LaunchedEffect(wallpapers, wallpaper) {
        if (wallpapers != null && wallpaper == null) onClose()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is WallpaperEvent.Applied) {
                val message = if (event.success) R.string.wallpapers_applied else R.string.wallpapers_apply_failed
                snackbarHostState.showSnackbar(context.getString(message))
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        WallpaperImage(
            wallpaper = wallpaper,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FilledTonalIconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            FilledTonalIconButton(onClick = { wallpaper?.let(viewModel::requestDelete) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            SnackbarHost(snackbarHostState)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.wallpapers_apply_title), style = MaterialTheme.typography.titleMedium)
                TargetSelector(selected = target, onSelect = { target = it }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAdjustCrop, enabled = wallpaper != null, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.action_adjust_crop))
                    }
                    Button(
                        onClick = { wallpaper?.let { viewModel.apply(it, target) } },
                        enabled = wallpaper != null && !applying,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.action_apply))
                    }
                }
            }
        }
    }

    pendingDeletion?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.wallpapers_delete_title)) },
            text = {
                Text(
                    if (pending.scheduleCount > 0) {
                        stringResource(R.string.wallpapers_delete_message_with_schedules, pending.scheduleCount)
                    } else {
                        stringResource(R.string.wallpapers_delete_message)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
