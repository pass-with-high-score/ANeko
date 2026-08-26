package org.nqmgaming.aneko.presentation.detail.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.pet.CodexPetContract
import org.nqmgaming.aneko.presentation.detail.SkinPreviewCache
import java.io.File

data class FrameItem(
    val label: String,
    val bitmap: Bitmap,
)

@Composable
fun SkinFramesGallery(
    packageName: String,
    accentColor: Color,
    onlineImageUrl: String? = null,
    downloadUrl: String? = null,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var framesList by remember(packageName, onlineImageUrl, downloadUrl) {
        mutableStateOf<List<FrameItem>>(emptyList())
    }

    LaunchedEffect(packageName, onlineImageUrl, downloadUrl) {
        val loaded = withContext(Dispatchers.IO) {
            extractAllSkinFrames(context, packageName, onlineImageUrl, downloadUrl)
        }
        framesList = loaded
    }

    if (framesList.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.skin_gallery_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.skin_total_frames, framesList.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(framesList, key = { it.label }) { frame ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(68.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 1.dp,
                                    color = accentColor.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!frame.bitmap.isRecycled) {
                                Image(
                                    bitmap = frame.bitmap.asImageBitmap(),
                                    contentDescription = frame.label,
                                    modifier = Modifier.size(52.dp),
                                    contentScale = ContentScale.Fit,
                                    filterQuality = FilterQuality.None,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = frame.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun extractAllSkinFrames(
    context: Context,
    packageName: String,
    onlineImageUrl: String?,
    downloadUrl: String?
): List<FrameItem> {
    var skinDir = resolveSkinDirectory(context, packageName)

    if (skinDir == null && !downloadUrl.isNullOrBlank()) {
        skinDir = SkinPreviewCache.getOrFetchDirectory(context, packageName, downloadUrl)
    }

    val hasLocalDir = skinDir != null && skinDir.exists()

    // ── Codex Pet Spritesheet Slicing ──
    val isCodex = (hasLocalDir && skinDir.walkTopDown().any { it.name.equals("pet.json", true) }) ||
            packageName.startsWith("codex.")

    if (isCodex && hasLocalDir) {
        val atlasFile = skinDir.walkTopDown().firstOrNull { file ->
            file.isFile && (file.name.equals("spritesheet.png", ignoreCase = true) ||
                    file.name.equals("spritesheet.webp", ignoreCase = true))
        }

        if (atlasFile != null) {
            val atlas = BitmapFactory.decodeFile(atlasFile.absolutePath)
            if (atlas != null) {
                val columns = 8
                val cellWidth = atlas.width / columns
                val rows = if (atlas.height >= cellWidth * 11) 11 else 9
                val cellHeight = atlas.height / rows
                val result = mutableListOf<FrameItem>()

                val rowNames = listOf(
                    0 to "idle",
                    1 to "right",
                    2 to "left",
                    3 to "wave",
                    4 to "jump",
                    5 to "failed",
                    6 to "wait",
                    7 to "run",
                    8 to "review",
                )

                for ((row, name) in rowNames) {
                    val y = row * cellHeight
                    if (y + cellHeight <= atlas.height) {
                        for (col in 0 until 4) {
                            val x = col * cellWidth
                            if (x + cellWidth <= atlas.width) {
                                val bmp = Bitmap.createBitmap(atlas, x, y, cellWidth, cellHeight)
                                result.add(FrameItem("$name-$col", bmp))
                            }
                        }
                    }
                }
                if (result.isNotEmpty()) return result
            }
        }
    }

    // ── Classic ANeko Skin files ──
    val allFiles = if (hasLocalDir) {
        skinDir.walkTopDown().filter { it.isFile && it.extension in listOf("png", "webp", "jpg") }.toList()
    } else {
        emptyList()
    }

    if (allFiles.isNotEmpty()) {
        return allFiles.sortedBy { it.name }.mapNotNull { f ->
            val bmp = BitmapFactory.decodeFile(f.absolutePath) ?: return@mapNotNull null
            FrameItem(f.nameWithoutExtension, bmp)
        }
    }

    // ── Online Skin Preview extraction fallback ──
    if (!onlineImageUrl.isNullOrBlank()) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(onlineImageUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                val bitmap = (drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val isPetdexStrip = onlineImageUrl.contains("preview.webp") || (bitmap.width >= bitmap.height * 3)
                    if (isPetdexStrip) {
                        val frameCount = 6
                        val frameWidth = bitmap.width / frameCount
                        val resultList = mutableListOf<FrameItem>()
                        for (col in 0 until frameCount) {
                            val startX = col * frameWidth
                            if (startX + frameWidth <= bitmap.width) {
                                val frameBmp = Bitmap.createBitmap(bitmap, startX, 0, frameWidth, bitmap.height)
                                resultList.add(FrameItem("idle-$col", frameBmp))
                            }
                        }
                        if (resultList.isNotEmpty()) return resultList
                    } else {
                        return listOf(FrameItem("preview", bitmap))
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Built-in assets fallback
    if (packageName == context.packageName || packageName == "org.nqmgaming.aneko") {
        try {
            val assetList = context.assets.list("aneko")?.filter { it.endsWith(".png") } ?: emptyList()
            return assetList.sorted().mapNotNull { assetName ->
                context.assets.open("aneko/$assetName").use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream) ?: return@mapNotNull null
                    FrameItem(assetName.substringBeforeLast('.'), bmp)
                }
            }
        } catch (_: Exception) {}
    }
    return emptyList()
}

private fun resolveSkinDirectory(context: Context, packageName: String): File? {
    val installedDir = File(File(context.filesDir, "skins"), packageName)
    if (installedDir.exists() && installedDir.isDirectory && installedDir.listFiles()?.isNotEmpty() == true) {
        return installedDir
    }
    val previewDir = File(File(context.cacheDir, "skin_previews"), packageName)
    if (previewDir.exists() && previewDir.isDirectory && previewDir.listFiles()?.isNotEmpty() == true) {
        return previewDir
    }
    return null
}
