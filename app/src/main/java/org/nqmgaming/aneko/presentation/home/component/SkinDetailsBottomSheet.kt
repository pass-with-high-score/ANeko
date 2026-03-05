package org.nqmgaming.aneko.presentation.home.component

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.data.entity.SkinEntity
import org.nqmgaming.aneko.core.data.entity.previewModel
import org.nqmgaming.aneko.core.shortcuts.ShortcutManagerHelper
import org.nqmgaming.aneko.core.util.extension.getStringResource
import org.nqmgaming.aneko.core.util.zipDirectory
import timber.log.Timber
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinDetailsBottomSheet(
    skin: SkinEntity,
    isBottomSheetVisible: Boolean,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    onRequestDeleteSkin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resource = LocalResources.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val model = remember(skin.packageName, skin.previewPath) {
        ImageRequest.Builder(context)
            .data(skin.previewModel(context))
            .crossfade(true)
            .build()
    }

    // Size slider state
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
    var sizeValue by remember {
        val perSkin = prefs.getString("motion.size.${skin.packageName}", null)
        val global = prefs.getString("motion.size", "80")
        val v = (perSkin ?: global)?.toFloatOrNull()?.coerceIn(sizeRange) ?: 80f
        mutableFloatStateOf(v)
    }

    // Transparency slider state
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
    var transparencyValue by remember {
        val perSkin = prefs.getString("motion.transparency.${skin.packageName}", null)
        val global = prefs.getString("motion.transparency", "0.0")
        val v = (perSkin ?: global)?.toFloatOrNull()?.coerceIn(transparencyRange) ?: 0f
        mutableFloatStateOf(v)
    }

    // Speed slider state
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
    var speedValue by remember {
        val perSkin = prefs.getString("motion.speed.${skin.packageName}", null)
        val global = prefs.getString("motion.speed", "1.0")
        val v = (perSkin ?: global)?.toFloatOrNull()?.coerceIn(speedRange) ?: 1.0f
        mutableFloatStateOf(v)
    }

    if (isBottomSheetVisible) {
        ModalBottomSheet(
            modifier = modifier,
            sheetState = bottomSheetState,
            onDismissRequest = onDismissRequest,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
            }
        ) {
            AnimatedVisibility(
                visible = isBottomSheetVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = model,
                            contentDescription = skin.name,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = skin.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.package_skin_label,
                                    skin.packageName
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.label_author, skin.author),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Divider
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // ── Motion Settings Section Title ──
                    Text(
                        text = stringResource(R.string.skin_settings_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // ── Size Slider ──
                    SkinSliderItem(
                        label = stringResource(R.string.skin_size_label),
                        value = sizeValue,
                        valueRange = sizeRange,
                        steps = sizeSteps,
                        displayText = {
                            val idx = sizeEntryValues.indexOfFirst { ev -> ev >= sizeValue }
                                .coerceIn(0, sizeEntries.lastIndex)
                            sizeEntries[idx]
                        },
                        onValueChange = { sizeValue = it },
                        onValueChangeFinished = {
                            val closest = sizeEntryValues.minByOrNull { abs(it - sizeValue) }
                                ?: sizeEntryValues.first()
                            sizeValue = closest
                            prefs.edit {
                                putString("motion.size.${skin.packageName}", closest.toString())
                            }
                        }
                    )

                    // ── Transparency Slider ──
                    SkinSliderItem(
                        label = stringResource(R.string.skin_transparency_label),
                        value = transparencyValue,
                        valueRange = transparencyRange,
                        steps = transparencySteps,
                        displayText = {
                            val idx =
                                transparencyEntryValues.indexOfFirst { ev -> ev >= transparencyValue }
                                    .coerceIn(0, transparencyEntries.lastIndex)
                            transparencyEntries[idx]
                        },
                        onValueChange = { transparencyValue = it },
                        onValueChangeFinished = {
                            val closest =
                                transparencyEntryValues.minByOrNull { abs(it - transparencyValue) }
                                    ?: transparencyEntryValues.first()
                            transparencyValue = closest
                            prefs.edit {
                                putString(
                                    "motion.transparency.${skin.packageName}",
                                    closest.toString()
                                )
                            }
                        }
                    )

                    // ── Speed Slider ──
                    SkinSliderItem(
                        label = stringResource(R.string.skin_speed_label),
                        value = speedValue,
                        valueRange = speedRange,
                        steps = speedSteps,
                        displayText = {
                            val idx = speedEntryValues.indexOfFirst { ev -> ev >= speedValue }
                                .coerceIn(0, speedEntries.lastIndex)
                            speedEntries[idx]
                        },
                        onValueChange = { speedValue = it },
                        onValueChangeFinished = {
                            val closest = speedEntryValues.minByOrNull { abs(it - speedValue) }
                                ?: speedEntryValues.first()
                            speedValue = closest
                            prefs.edit {
                                putString("motion.speed.${skin.packageName}", closest.toString())
                            }
                        }
                    )

                    // Divider
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Delete & Share Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                onDismissRequest()
                                onRequestDeleteSkin()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.uninstall),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Button(
                            onClick = {
                                try {
                                    val skinDir =
                                        File(context.filesDir, "skins/${skin.packageName}")

                                    val zipFile = File(
                                        context.cacheDir,
                                        "${skin.name}.zip"
                                    )
                                    if (zipFile.exists()) {
                                        zipFile.delete()
                                    }
                                    zipFile.outputStream().use { outputStream ->
                                        zipDirectory(
                                            skinDir,
                                            outputStream
                                        )
                                    }
                                    val zipUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        zipFile
                                    )

                                    val shareIntent =
                                        android.content.Intent(android.content.Intent.ACTION_SEND)
                                            .apply {
                                                type = "application/zip"
                                                putExtra(
                                                    android.content.Intent.EXTRA_STREAM,
                                                    zipUri
                                                )
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                    context.startActivity(
                                        android.content.Intent.createChooser(
                                            shareIntent,
                                            context.getStringResource(R.string.share_skin_label)
                                        )
                                    )
                                } catch (e: Exception) {
                                    Timber.e(e, "Error sharing skin: ${skin.name}")
                                    Toast.makeText(
                                        context,
                                        context.getStringResource(
                                            R.string.failed_to_share_apk_label,
                                            skin.name
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.share_skin_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                        }
                    }

                    // Pin Shortcut Button
                    Button(
                        onClick = {
                            try {
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    ShortcutManagerHelper.createPinnedSkinShortcut(
                                        context = context,
                                        skinName = skin.name,
                                        skinPackage = skin.packageName,
                                        previewPath = skin.previewPath
                                    )
                                    Toast.makeText(
                                        context,
                                        context.getStringResource(R.string.message_shortcut_request_sent),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismissRequest()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getStringResource(R.string.message_unable_create_shortcut),
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
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.button_pin_shortcut),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayText: () -> String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = displayText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                activeTickColor = MaterialTheme.colorScheme.primary,
                inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
            )
        )
    }
}