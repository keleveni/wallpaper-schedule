package com.keleveni.wallpaperscheduler.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.keleveni.wallpaperscheduler.R
import com.keleveni.wallpaperscheduler.WallpaperSchedulerApp
import com.keleveni.wallpaperscheduler.data.Folder
import com.keleveni.wallpaperscheduler.data.WallpaperEntity
import com.keleveni.wallpaperscheduler.data.WallpaperTarget

/** 壁紙の画像。表示範囲が設定されていれば、その範囲だけを表示する。 */
@Composable
fun WallpaperImage(
    wallpaper: WallpaperEntity?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (wallpaper != null) {
            val file = (context.applicationContext as WallpaperSchedulerApp).container.wallpapers.fileOf(wallpaper)
            val request = remember(file, wallpaper.crop) {
                ImageRequest.Builder(context)
                    .data(file)
                    .apply { wallpaper.crop?.let { transformations(CropTransformation(it)) } }
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** フォルダー内の最新の壁紙を表紙として表示する。[showLabel] が true なら名前と枚数も重ねて表示する。 */
@Composable
fun FolderCover(
    folder: Folder,
    wallpapers: List<WallpaperEntity>,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val cover = wallpapers.firstOrNull { it.id in folder.wallpaperIds }
    Box(modifier) {
        WallpaperImage(cover, Modifier.matchParentSize())
        Icon(
            Icons.Default.Shuffle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.4f), MaterialTheme.shapes.small)
                .padding(2.dp)
                .size(16.dp),
        )
        if (showLabel) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    folder.name,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.folder_count, folder.wallpaperIds.size),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@get:StringRes
val WallpaperTarget.label: Int
    get() = when (this) {
        WallpaperTarget.HOME -> R.string.target_home
        WallpaperTarget.LOCK -> R.string.target_lock
        WallpaperTarget.BOTH -> R.string.target_both
    }

@Composable
fun TargetSelector(
    selected: WallpaperTarget,
    onSelect: (WallpaperTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targets = WallpaperTarget.entries
    SingleChoiceSegmentedButtonRow(modifier) {
        targets.forEachIndexed { index, target ->
            SegmentedButton(
                selected = target == selected,
                onClick = { onSelect(target) },
                shape = SegmentedButtonDefaults.itemShape(index, targets.size),
            ) {
                Text(stringResource(target.label))
            }
        }
    }
}

fun formatTime(hour: Int, minute: Int): String = "%d:%02d".format(hour, minute)
