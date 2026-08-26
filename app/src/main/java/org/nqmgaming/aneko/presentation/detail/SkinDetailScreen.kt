package org.nqmgaming.aneko.presentation.detail

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.preference.PreferenceManager
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.download.DownloadStatus
import org.nqmgaming.aneko.core.download.DownloadTask
import org.nqmgaming.aneko.core.download.SkinDownloadQueue
import org.nqmgaming.aneko.core.shortcuts.ShortcutManagerHelper
import org.nqmgaming.aneko.core.util.extension.getStringResource
import org.nqmgaming.aneko.core.util.zipDirectory
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.detail.component.SkinAnimationPlayer
import org.nqmgaming.aneko.presentation.detail.component.SkinFramesGallery
import org.nqmgaming.aneko.presentation.detail.component.SkinMotionSettingsSection
import org.nqmgaming.aneko.presentation.detail.component.SkinSpecsSection
import timber.log.Timber
import java.io.File
import kotlin.math.abs

private val collectibleCardColors = listOf(
    Color(0xFF26A69A), // Teal
    Color(0xFF5C6BC0), // Indigo
    Color(0xFFAB47BC), // Purple
    Color(0xFFEC407A), // Pink
    Color(0xFFEF5350), // Red
    Color(0xFF42A5F5), // Blue
    Color(0xFFFF7043), // Deep Orange
    Color(0xFF66BB6A), // Green
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinDetailScreen(
    packageName: String,
    onNavigateBack: () -> Unit,
    viewModel: AnekoViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resource = LocalResources.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val uiState by viewModel.uiState.collectAsState()

    val localSkin = uiState.skins.find { it.packageName == packageName }
    val onlineSkin = (uiState.skinCollections.orEmpty() + uiState.petdexCollections.orEmpty())
        .find { it.packageName == packageName }

    val isInstalled = localSkin != null
    val isActive = localSkin?.isActive == true
    val isBuiltin = localSkin?.isBuiltin == true || (onlineSkin?.isBuiltIn == true)
    val isCodex = packageName.startsWith("codex.") || (onlineSkin?.isPetdex == true)

    val displayName = localSkin?.name ?: onlineSkin?.name ?: packageName
    val author = localSkin?.author ?: onlineSkin?.author ?: ""
    val version = localSkin?.version ?: onlineSkin?.version ?: ""

    val cardColor = remember(packageName) {
        collectibleCardColors[abs(packageName.hashCode()) % collectibleCardColors.size]
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // ── Motion Sliders State ──
    val sizeEntryValues = remember {
        resource.getStringArray(R.array.pref_motion_size_entry_values)
            .mapNotNull { it.toFloatOrNull() }
    }
    val sizeEntries = remember {
        resource.getStringArray(R.array.pref_motion_size)
    }
    val sizeRange = remember(sizeEntryValues) {
        sizeEntryValues.min()..sizeEntryValues.max()
    }
    val sizeSteps = remember(sizeEntryValues) { sizeEntryValues.size - 2 }
    var sizeValue by remember(packageName) {
        val perSkin = prefs.getString("motion.size.$packageName", null)
        val global = prefs.getString("motion.size", "80")
        val v = (perSkin ?: global)?.toFloatOrNull()?.coerceIn(sizeRange) ?: 80f
        mutableFloatStateOf(v)
    }

    val transparencyEntryValues = remember {
        resource.getStringArray(R.array.pref_motion_transparency_entry_values)
            .mapNotNull { it.toFloatOrNull() }
    }
    val transparencyEntries = remember {
        resource.getStringArray(R.array.pref_motion_transparency_entries)
    }
    val transparencyRange = remember(transparencyEntryValues) {
        transparencyEntryValues.min()..transparencyEntryValues.max()
    }
    val transparencySteps = remember(transparencyEntryValues) { transparencyEntryValues.size - 2 }
    var transparencyValue by remember(packageName) {
        val perSkin = prefs.getString("motion.transparency.$packageName", null)
        val global = prefs.getString("motion.transparency", "0.0")
        val v = (perSkin ?: global)?.toFloatOrNull()?.coerceIn(transparencyRange) ?: 0f
        mutableFloatStateOf(v)
    }

    val speedEntryValues = remember {
        resource.getStringArray(R.array.pref_motion_speed_entry_values)
            .mapNotNull { it.toFloatOrNull() }
    }
    val speedEntries = remember {
        resource.getStringArray(R.array.pref_motion_speed_entries)
    }
    val speedRange = remember(speedEntryValues) {
        speedEntryValues.min()..speedEntryValues.max()
    }
    val speedSteps = remember(speedEntryValues) { speedEntryValues.size - 2 }
    var speedValue by remember(packageName) {
        val perSkin = prefs.getString("motion.speed.$packageName", null)
        val global = prefs.getString("motion.speed", "1.0")
        val v = (perSkin ?: global)?.toFloatOrNull()?.coerceIn(speedRange) ?: 1.0f
        mutableFloatStateOf(v)
    }

    // Download Queue Status for online skin
    val statusMap by SkinDownloadQueue.status.collectAsState()
    val downloadStatus = statusMap[packageName] ?: DownloadStatus.Idle
    val queuePos = SkinDownloadQueue.queuePositionOf(packageName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (author.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.label_author, author),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isInstalled) {
                        IconButton(onClick = {
                            shareSkin(context, packageName, displayName)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share_skin_label)
                            )
                        }
                        IconButton(onClick = {
                            createShortcut(context, displayName, packageName, localSkin.previewPath)
                        }) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = stringResource(R.string.button_pin_shortcut)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isInstalled) {
                        // Toggle Active Button
                        Button(
                            onClick = {
                                viewModel.onToggleSkin(packageName, context)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.errorContainer
                                else cardColor,
                                contentColor = if (isActive) MaterialTheme.colorScheme.onErrorContainer
                                else Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isActive) stringResource(R.string.skin_action_remove_active)
                                else stringResource(R.string.skin_action_set_active),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Uninstall Button (if not built-in)
                        if (!isBuiltin) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.uninstall),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else if (onlineSkin != null) {
                        // Download / Install Button
                        val (label, icon) = when (downloadStatus) {
                            is DownloadStatus.Idle -> stringResource(R.string.download) to Icons.Default.Download
                            is DownloadStatus.Queued -> "${stringResource(R.string.queued)} #$queuePos" to Icons.Default.Download
                            is DownloadStatus.Downloading -> "${stringResource(R.string.downloading)} ${downloadStatus.progressPct}%" to Icons.Default.Download
                            is DownloadStatus.Importing -> stringResource(R.string.importing) to Icons.Default.Download
                            is DownloadStatus.Done -> stringResource(R.string.installed) to Icons.Default.Check
                            is DownloadStatus.Failed -> stringResource(R.string.retry) to Icons.Default.Update
                        }

                        Button(
                            onClick = {
                                if (downloadStatus is DownloadStatus.Idle || downloadStatus is DownloadStatus.Failed) {
                                    SkinDownloadQueue.enqueue(
                                        context = context,
                                        task = DownloadTask(
                                            id = onlineSkin.packageName,
                                            url = onlineSkin.url,
                                            fileName = onlineSkin.packageName,
                                            codexPetId = onlineSkin.codexPetId,
                                            author = onlineSkin.author,
                                            version = onlineSkin.version,
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cardColor,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = icon, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Live Animation Playground Canvas ──
            SkinAnimationPlayer(
                packageName = packageName,
                cardColor = cardColor,
                scaleFactor = (sizeValue / 80f).coerceIn(0.6f, 1.8f),
                transparencyAlpha = (1.0f - (transparencyValue.coerceIn(0f, 0.8f))),
                speedMultiplier = speedValue.coerceIn(0.5f, 2.0f),
                onlineImageUrl = onlineSkin?.image,
                downloadUrl = onlineSkin?.url,
                isOnline = !isInstalled,
            )

            // Status chip badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isActive) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.skin_active_status)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            if (isCodex) stringResource(R.string.skin_engine_codex)
                            else stringResource(R.string.skin_engine_aneko)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                if (isBuiltin) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.skin_source_official)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ── Per-Skin Motion Settings ──
            if (isInstalled) {
                val sizeIdx = sizeEntryValues.indexOfFirst { ev -> ev >= sizeValue }
                    .coerceIn(0, sizeEntries.lastIndex)
                val transIdx = transparencyEntryValues.indexOfFirst { ev -> ev >= transparencyValue }
                    .coerceIn(0, transparencyEntries.lastIndex)
                val speedIdx = speedEntryValues.indexOfFirst { ev -> ev >= speedValue }
                    .coerceIn(0, speedEntries.lastIndex)

                SkinMotionSettingsSection(
                    sizeValue = sizeValue,
                    sizeRange = sizeRange,
                    sizeSteps = sizeSteps,
                    sizeDisplay = sizeEntries[sizeIdx],
                    onSizeChange = { sizeValue = it },
                    onSizeChangeFinished = {
                        val closest = sizeEntryValues.minByOrNull { abs(it - sizeValue) } ?: sizeEntryValues.first()
                        sizeValue = closest
                        prefs.edit { putString("motion.size.$packageName", closest.toString()) }
                    },
                    transparencyValue = transparencyValue,
                    transparencyRange = transparencyRange,
                    transparencySteps = transparencySteps,
                    transparencyDisplay = transparencyEntries[transIdx],
                    onTransparencyChange = { transparencyValue = it },
                    onTransparencyChangeFinished = {
                        val closest = transparencyEntryValues.minByOrNull { abs(it - transparencyValue) } ?: transparencyEntryValues.first()
                        transparencyValue = closest
                        prefs.edit { putString("motion.transparency.$packageName", closest.toString()) }
                    },
                    speedValue = speedValue,
                    speedRange = speedRange,
                    speedSteps = speedSteps,
                    speedDisplay = speedEntries[speedIdx],
                    onSpeedChange = { speedValue = it },
                    onSpeedChangeFinished = {
                        val closest = speedEntryValues.minByOrNull { abs(it - speedValue) } ?: speedEntryValues.first()
                        speedValue = closest
                        prefs.edit { putString("motion.speed.$packageName", closest.toString()) }
                    },
                    accentColor = cardColor
                )
            }

            // ── Sprite & Frame Gallery ──
            SkinFramesGallery(
                packageName = packageName,
                accentColor = cardColor,
                onlineImageUrl = onlineSkin?.image,
                downloadUrl = onlineSkin?.url,
                isOnline = !isInstalled,
            )

            // ── Technical Specifications ──
            SkinSpecsSection(
                packageName = packageName,
                author = author,
                version = version,
                isCodex = isCodex,
                isBuiltin = isBuiltin,
                isInstalled = isInstalled,
                isActive = isActive
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Delete confirmation dialog
        if (showDeleteConfirmDialog && localSkin != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text(stringResource(R.string.delete_skin_confirm_title)) },
                text = { Text(stringResource(R.string.delete_skin_confirm_msg, displayName)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.onDeselectSkin(localSkin, context)
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

private fun shareSkin(context: android.content.Context, packageName: String, displayName: String) {
    try {
        val skinDir = File(context.filesDir, "skins/$packageName")
        val zipFile = File(context.cacheDir, "$displayName.zip")
        if (zipFile.exists()) {
            zipFile.delete()
        }
        zipFile.outputStream().use { outputStream ->
            zipDirectory(skinDir, outputStream)
        }
        val zipUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            zipFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, zipUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, context.getStringResource(R.string.share_skin_label))
        )
    } catch (e: Exception) {
        Timber.e(e, "Error sharing skin: $displayName")
        Toast.makeText(
            context,
            context.getStringResource(R.string.failed_to_share_apk_label, displayName),
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun createShortcut(context: android.content.Context, name: String, packageName: String, previewPath: String) {
    try {
        val activity = context as? Activity
        if (activity != null) {
            ShortcutManagerHelper.createPinnedSkinShortcut(
                context = context,
                skinName = name,
                skinPackage = packageName,
                previewPath = previewPath
            )
            Toast.makeText(
                context,
                context.getStringResource(R.string.message_shortcut_request_sent),
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (e: Exception) {
        Timber.e(e, "Error creating skin shortcut")
        Toast.makeText(
            context,
            context.getStringResource(R.string.message_failed_to_create_shortcut),
            Toast.LENGTH_SHORT
        ).show()
    }
}
