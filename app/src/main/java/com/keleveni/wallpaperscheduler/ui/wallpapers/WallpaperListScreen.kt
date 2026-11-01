package com.keleveni.wallpaperscheduler.ui.wallpapers

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keleveni.wallpaperscheduler.R
import com.keleveni.wallpaperscheduler.data.Folder
import com.keleveni.wallpaperscheduler.data.WallpaperEntity
import com.keleveni.wallpaperscheduler.ui.components.WallpaperImage

/** フォルダー名の入力ダイアログの状態。[folder] が null なら新規作成。 */
private data class NameDialog(val folder: Folder?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperListScreen(
    onOpenWallpaper: (Long) -> Unit,
    viewModel: WallpaperListViewModel = viewModel(factory = WallpaperListViewModel.Factory),
) {
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val pendingFolderDeletion by viewModel.pendingFolderDeletion.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // null は「すべて」
    var selectedFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedFolder = folders.find { it.id == selectedFolderId }
    var nameDialog by remember { mutableStateOf<NameDialog?>(null) }
    var selectingWallpapers by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // 写真選択ツールを使うため、ストレージの読み取り権限は不要
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) viewModel.import(uris, selectedFolder?.id)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                WallpaperEvent.ImportFailed ->
                    snackbarHostState.showSnackbar(context.getString(R.string.wallpapers_import_failed))
                is WallpaperEvent.FolderCreated -> selectedFolderId = event.folderId
                is WallpaperEvent.Applied -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wallpapers_title)) },
                actions = {
                    if (selectedFolder != null) {
                        IconButton(onClick = { selectingWallpapers = true }) {
                            Icon(Icons.Default.LibraryAdd, contentDescription = stringResource(R.string.folder_select_wallpapers))
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.folder_rename)) },
                                    onClick = {
                                        menuExpanded = false
                                        nameDialog = NameDialog(selectedFolder)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.folder_delete)) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.requestDeleteFolder(selectedFolder)
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FolderChips(
                folders = folders,
                selected = selectedFolder,
                onSelect = { selectedFolderId = it?.id },
                onCreate = { nameDialog = NameDialog(null) },
            )
            val list = wallpapers
            val shown = if (selectedFolder == null) list else list?.filter { it.id in selectedFolder.wallpaperIds }
            if (shown != null && shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(if (selectedFolder == null) R.string.wallpapers_empty else R.string.folder_empty),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shown.orEmpty(), key = { it.id }) { wallpaper ->
                        WallpaperImage(
                            wallpaper = wallpaper,
                            modifier = Modifier
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenWallpaper(wallpaper.id) },
                        )
                    }
                }
            }
        }
    }

    nameDialog?.let { dialog ->
        FolderNameDialog(
            initialName = dialog.folder?.name.orEmpty(),
            title = stringResource(if (dialog.folder == null) R.string.folder_create else R.string.folder_rename),
            onDismiss = { nameDialog = null },
            onConfirm = { name ->
                if (dialog.folder == null) viewModel.createFolder(name) else viewModel.renameFolder(dialog.folder, name)
                nameDialog = null
            },
        )
    }

    if (selectingWallpapers && selectedFolder != null) {
        SelectWallpapersDialog(
            wallpapers = wallpapers.orEmpty(),
            initialSelection = selectedFolder.wallpaperIds,
            onDismiss = { selectingWallpapers = false },
            onConfirm = { ids ->
                viewModel.setFolderWallpapers(selectedFolder, ids)
                selectingWallpapers = false
            },
        )
    }

    pendingFolderDeletion?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteFolder,
            title = { Text(stringResource(R.string.folder_delete)) },
            text = {
                Text(
                    if (pending.scheduleCount > 0) {
                        stringResource(R.string.folder_delete_message_with_schedules, pending.folder.name, pending.scheduleCount)
                    } else {
                        stringResource(R.string.folder_delete_message, pending.folder.name)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmDeleteFolder()
                    selectedFolderId = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteFolder) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun FolderChips(
    folders: List<Folder>,
    selected: Folder?,
    onSelect: (Folder?) -> Unit,
    onCreate: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.folder_all)) },
            )
        }
        items(folders, key = { it.id }) { folder ->
            FilterChip(
                selected = folder.id == selected?.id,
                onClick = { onSelect(folder) },
                label = { Text(stringResource(R.string.folder_chip, folder.name, folder.wallpaperIds.size)) },
            )
        }
        item {
            AssistChip(
                onClick = onCreate,
                label = { Text(stringResource(R.string.folder_create)) },
                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun FolderNameDialog(
    initialName: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.folder_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** フォルダーに入れる壁紙を、登録済みの壁紙から複数選ぶ。 */
@Composable
private fun SelectWallpapersDialog(
    wallpapers: List<WallpaperEntity>,
    initialSelection: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit,
) {
    var selection by rememberSaveable { mutableStateOf(initialSelection.toList()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_select_wallpapers)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 72.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(wallpapers, key = { it.id }) { wallpaper ->
                    val checked = wallpaper.id in selection
                    val shape = RoundedCornerShape(8.dp)
                    Box(
                        Modifier
                            .aspectRatio(9f / 16f)
                            .clip(shape)
                            .then(
                                if (checked) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), shape) else Modifier,
                            )
                            .clickable { selection = if (checked) selection - wallpaper.id else selection + wallpaper.id },
                    ) {
                        WallpaperImage(wallpaper, Modifier.matchParentSize())
                        Icon(
                            if (checked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = if (checked) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selection.toSet()) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
