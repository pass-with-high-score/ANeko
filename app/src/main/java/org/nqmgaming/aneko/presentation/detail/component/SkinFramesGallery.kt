package org.nqmgaming.aneko.presentation.detail.component

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.pet.CodexPetContract
import org.nqmgaming.aneko.core.util.extension.getStringResource
import org.nqmgaming.aneko.presentation.detail.SkinPreviewCache
import timber.log.Timber
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
    var selectedFrame by remember { mutableStateOf<FrameItem?>(null) }

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
                                )
                                .clickable { selectedFrame = frame },
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

    selectedFrame?.let { frame ->
        FramePreviewDialog(
            frame = frame,
            accentColor = accentColor,
            onDismiss = { selectedFrame = null }
        )
    }
}

@Composable
private fun FramePreviewDialog(
    frame: FrameItem,
    accentColor: Color,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch { saveFrameAndNotify(context, frame) }
        } else {
            Toast.makeText(
                context,
                context.getStringResource(R.string.storage_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun handleSave() {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            scope.launch { saveFrameAndNotify(context, frame) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.frame_preview_title, frame.label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!frame.bitmap.isRecycled) {
                        Image(
                            bitmap = frame.bitmap.asImageBitmap(),
                            contentDescription = frame.label,
                            modifier = Modifier.size(140.dp),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { handleSave() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.save_frame_label))
                    }
                    Button(
                        onClick = {
                            shareFrame(context, frame.bitmap, frame.label)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.share_frame_label))
                    }
                }
            }
        }
    }
}

private suspend fun saveFrameAndNotify(context: Context, frame: FrameItem) {
    val success = withContext(Dispatchers.IO) {
        saveFrameToGallery(context, frame.bitmap, frame.label)
    }
    Toast.makeText(
        context,
        context.getStringResource(if (success) R.string.frame_saved_success else R.string.frame_saved_failed),
        Toast.LENGTH_SHORT
    ).show()
}

private fun saveFrameToGallery(context: Context, bitmap: Bitmap, label: String): Boolean {
    val fileName = "aneko_frame_$label.png"
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ANeko")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: return false
        } else {
            @Suppress("DEPRECATION")
            val picturesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ANeko"
            )
            if (!picturesDir.exists()) picturesDir.mkdirs()
            val file = File(picturesDir, fileName)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
        }
        true
    } catch (e: Exception) {
        Timber.e(e, "Error saving frame: $label")
        false
    }
}

private fun shareFrame(context: Context, bitmap: Bitmap, label: String) {
    try {
        val exportsDir = File(context.cacheDir, "frame_exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()
        val file = File(exportsDir, "aneko_frame_$label.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, context.getStringResource(R.string.share_frame_label))
        )
    } catch (e: Exception) {
        Timber.e(e, "Error sharing frame: $label")
        Toast.makeText(
            context,
            context.getStringResource(R.string.frame_share_failed),
            Toast.LENGTH_SHORT
        ).show()
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
