package com.keleveni.wallpaperscheduler.ui.wallpapers

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.layout.WindowMetricsCalculator
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.keleveni.wallpaperscheduler.R
import com.keleveni.wallpaperscheduler.data.CropRect
import kotlin.math.min

/** 壁紙として表示する範囲を、画面と同じ縦横比の枠で選ぶ画面。 */
@Composable
fun WallpaperCropScreen(
    wallpaperId: Long,
    onClose: () -> Unit,
    viewModel: WallpaperListViewModel = viewModel(factory = WallpaperListViewModel.Factory),
) {
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    val wallpaper = wallpapers?.find { it.id == wallpaperId }
    LaunchedEffect(wallpapers, wallpaper) {
        if (wallpapers != null && wallpaper == null) onClose()
    }
    if (wallpaper == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val context = LocalContext.current
    val frameAspect = remember(context) { screenAspectRatio(context) }
    val file = remember(wallpaper.fileName) { viewModel.fileOf(wallpaper) }
    val painter = rememberAsyncImagePainter(
        remember(file) { ImageRequest.Builder(context).data(file).size(2048).build() },
    )
    val imageSize = painter.intrinsicSize
    val imageAspect = if (painter.state is AsyncImagePainter.State.Success && imageSize.isSpecified) {
        imageSize.width / imageSize.height
    } else {
        null
    }

    // 壁紙を適用した際などに Activity が再生成されても、編集中の範囲を保持する
    var crop by rememberSaveable(stateSaver = CropRectSaver) { mutableStateOf<CropRect?>(null) }
    LaunchedEffect(imageAspect) {
        if (imageAspect != null && crop == null) {
            val saved = wallpaper.crop
            crop = if (saved != null) {
                CropRect.fit(imageAspect, frameAspect, saved.centerX, saved.centerY, saved.width)
            } else {
                CropRect.fit(imageAspect, frameAspect)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(
                stringResource(R.string.crop_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val margin = with(LocalDensity.current) { 24.dp.toPx() }
            val areaWidth = constraints.maxWidth.toFloat()
            val areaHeight = constraints.maxHeight.toFloat()
            val frameWidth = min(areaWidth - margin * 2, (areaHeight - margin * 2) * frameAspect)
            val frameHeight = frameWidth / frameAspect
            val frame = Rect(
                offset = Offset((areaWidth - frameWidth) / 2, (areaHeight - frameHeight) / 2),
                size = Size(frameWidth, frameHeight),
            )
            val current = crop
            if (current == null || imageAspect == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            } else {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(frame, imageAspect, frameAspect) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val c = crop ?: return@detectTransformGestures
                                // 指の位置にある画像上の点を固定したまま拡大・縮小し、指の移動に合わせて画像を動かす
                                val fx = (centroid.x - frame.left) / frame.width
                                val fy = (centroid.y - frame.top) / frame.height
                                val anchorX = c.left + fx * c.width
                                val anchorY = c.top + fy * c.height
                                val width = c.width / zoom
                                val height = width * c.height / c.width
                                val left = anchorX - fx * width - pan.x / frame.width * width
                                val top = anchorY - fy * height - pan.y / frame.height * height
                                crop = CropRect.fit(imageAspect, frameAspect, left + width / 2, top + height / 2, width)
                            }
                        },
                ) {
                    val drawnWidth = frame.width / current.width
                    val drawnHeight = frame.height / current.height
                    translate(frame.left - current.left * drawnWidth, frame.top - current.top * drawnHeight) {
                        with(painter) { draw(Size(drawnWidth, drawnHeight)) }
                    }
                    // 枠の外側を暗くする
                    val dim = Color.Black.copy(alpha = 0.6f)
                    drawRect(dim, Offset.Zero, Size(size.width, frame.top))
                    drawRect(dim, Offset(0f, frame.bottom), Size(size.width, size.height - frame.bottom))
                    drawRect(dim, Offset(0f, frame.top), Size(frame.left, frame.height))
                    drawRect(dim, Offset(frame.right, frame.top), Size(size.width - frame.right, frame.height))
                    drawRect(Color.White, frame.topLeft, frame.size, style = Stroke(2.dp.toPx()))
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.crop_hint), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { imageAspect?.let { crop = CropRect.fit(it, frameAspect) } },
                    enabled = imageAspect != null,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_reset)) }
                Button(
                    onClick = {
                        viewModel.saveCrop(wallpaper, crop)
                        onClose()
                    },
                    enabled = crop != null,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}

/** 画面全体（ステータスバー等を含む）の縦向きの縦横比（幅 / 高さ）。壁紙はこの比率で表示される。 */
private fun screenAspectRatio(context: Context): Float {
    val bounds = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(context).bounds
    return minOf(bounds.width(), bounds.height()).toFloat() / maxOf(bounds.width(), bounds.height())
}

private val CropRectSaver = listSaver<CropRect?, Float>(
    save = { crop -> crop?.let { listOf(it.left, it.top, it.right, it.bottom) } ?: emptyList() },
    restore = { values -> if (values.size == 4) CropRect(values[0], values[1], values[2], values[3]) else null },
)
