package com.keleveni.wallpaperscheduler.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.keleveni.wallpaperscheduler.R
import com.keleveni.wallpaperscheduler.WallpaperSchedulerApp
import com.keleveni.wallpaperscheduler.data.WallpaperEntity
import com.keleveni.wallpaperscheduler.data.WallpaperTarget

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
            AsyncImage(
                model = file,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
            )
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
