package org.nqmgaming.aneko.presentation.edit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.create.FramesPreview
import org.nqmgaming.aneko.presentation.components.SectionCard
import coil.compose.AsyncImage
import timber.log.Timber
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun EditSkinScreen(
    navigator: DestinationsNavigator,
    packageName: String,
    viewModel: AnekoViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var previewPath by remember { mutableStateOf<String?>(null) }
    var newPreviewUri by remember { mutableStateOf<Uri?>(null) }

    val frameFiles = remember { mutableStateListOf<File>() }
    val newFrameUris = remember { mutableStateListOf<Uri>() }

    LaunchedEffect(packageName) {
        try {
            val dir = File(context.filesDir, "skins/$packageName")
            val xml = File(dir, "skin.xml")
            val meta = if (xml.exists()) viewModel.parseSkinMetadata(xml, context) else null
            name = meta?.name ?: packageName
            author = meta?.author ?: ""
            previewPath = meta?.previewPath
            val images = dir.listFiles { f ->
                f.isFile && f.name != "skin.xml" && (f.extension.lowercase() in listOf(
                    "png",
                    "jpg",
                    "jpeg",
                    "webp"
                ))
            }?.sortedBy { it.name } ?: emptyList()
            frameFiles.clear()
            // Exclude preview image from frames
            frameFiles.addAll(images.filterNot { it.name == previewPath })
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    val pickPreview = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            persistUri(context, uri)
            newPreviewUri = uri
        }
    }

    val pickFrames = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            persistUri(context, uri)
            newFrameUris.add(uri)
        }
    }

    // Preview setup: map to Any for Coil
    val previewFrames = remember(frameFiles, newFrameUris) {
        val list = mutableListOf<Any>()
        list.addAll(frameFiles)
        list.addAll(newFrameUris)
        list.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.edit_skin_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onSaveClicked(
                            context = context,
                            viewModel = viewModel,
                            packageName = packageName,
                            name = name,
                            author = author,
                            existingFrameFiles = frameFiles.toList(),
                            newFrameUris = newFrameUris.toList(),
                            newPreviewUri = newPreviewUri,
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.saved_skin_success, it),
                                    Toast.LENGTH_SHORT
                                ).show()
                                navigator.popBackStack()
                            }
                        )
                    }) { Icon(imageVector = Icons.Default.Check, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = stringResource(R.string.section_details)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.skin_name_label)) })
                    OutlinedTextField(value = packageName, onValueChange = {}, enabled = false, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.skin_package_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                    OutlinedTextField(value = author, onValueChange = { author = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.skin_author_label)) })
                }
            }

            SectionCard(title = stringResource(R.string.section_assets)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { pickPreview.launch(arrayOf("image/*")) }) { Text(stringResource(R.string.pick_preview_image)) }
                        OutlinedButton(onClick = { pickFrames.launch(arrayOf("image/*")) }) { Text(stringResource(R.string.add_frames_button)) }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        frameFiles.forEachIndexed { index, file ->
                            ListItem(
                                headlineContent = { Text(file.name) },
                                leadingContent = { AsyncImage(model = file, contentDescription = null) },
                                trailingContent = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { if (index > 0) { val r = frameFiles.removeAt(index); frameFiles.add(index - 1, r) } }) { Icon(Icons.Default.KeyboardArrowLeft, null) }
                                        IconButton(onClick = { if (index < frameFiles.lastIndex) { val r = frameFiles.removeAt(index); frameFiles.add(index + 1, r) } }) { Icon(Icons.Default.KeyboardArrowRight, null) }
                                        IconButton(onClick = { frameFiles.removeAt(index) }) { Icon(Icons.Default.Delete, null) }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            SectionCard(title = stringResource(R.string.section_preview)) {
                var frameMs by remember { mutableStateOf(250L) }
                var previewSize by remember { mutableStateOf(120f) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FramesPreview(frames = previewFrames, frameDurationMs = frameMs, size = previewSize.dp)
                    Text(stringResource(R.string.label_speed))
                    Slider(value = frameMs.toFloat(), onValueChange = { frameMs = it.coerceIn(50f, 1000f).toLong() }, valueRange = 50f..1000f)
                    Text(stringResource(R.string.label_size))
                    Slider(value = previewSize, onValueChange = { previewSize = it.coerceIn(64f, 240f) }, valueRange = 64f..240f)
                }
            }

            // Reorder/remove existing frame files
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                frameFiles.forEachIndexed { index, file ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "${index + 1}.")
                        IconButton(onClick = {
                            if (index > 0) {
                                val removed = frameFiles.removeAt(index)
                                frameFiles.add(index - 1, removed)
                            }
                        }) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null) }
                        IconButton(onClick = {
                            if (index < frameFiles.lastIndex) {
                                val removed = frameFiles.removeAt(index)
                                frameFiles.add(index + 1, removed)
                            }
                        }) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
                        IconButton(onClick = { frameFiles.removeAt(index) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null
                            )
                        }
                        Text(text = file.name, modifier = Modifier.weight(1f))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                    prefs.edit().putString(org.nqmgaming.aneko.core.service.AnimationService.PREF_KEY_SKIN_COMPONENT, packageName).apply()
                    prefs.edit().putBoolean(org.nqmgaming.aneko.core.service.AnimationService.PREF_KEY_VISIBLE, true).apply()
                    context.startService(
                        Intent(context, org.nqmgaming.aneko.core.service.AnimationService::class.java)
                            .setAction(org.nqmgaming.aneko.core.service.AnimationService.ACTION_START)
                    )
                }) { Text(text = stringResource(R.string.preview_overlay_start)) }

                Button(
                onClick = {
                    onSaveClicked(
                        context,
                        viewModel,
                        packageName,
                        name,
                        author,
                        frameFiles.toList(),
                        newFrameUris.toList(),
                        newPreviewUri
                    ) { savedPkg ->
                        Toast.makeText(
                            context,
                            context.getString(R.string.saved_skin_success, savedPkg),
                            Toast.LENGTH_SHORT
                        ).show()
                        navigator.popBackStack()
                    }
                },
                enabled = name.isNotBlank() && packageName.isNotBlank() && (frameFiles.isNotEmpty() || newFrameUris.isNotEmpty())
                ) { Text(text = stringResource(R.string.save_changes)) }
            }
        }
    }
}

private fun persistUri(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (e: SecurityException) {
        Timber.e(e, "Failed to persist URI permission")
    }
}

private fun onSaveClicked(
    context: Context,
    viewModel: AnekoViewModel,
    packageName: String,
    name: String,
    author: String,
    existingFrameFiles: List<File>,
    newFrameUris: List<Uri>,
    newPreviewUri: Uri?,
    onSuccess: (String) -> Unit,
) {
    viewModel.updateSkin(
        context = context,
        packageName = packageName,
        name = name,
        author = author,
        existingFrameFiles = existingFrameFiles,
        newFrameUris = newFrameUris,
        newPreviewUri = newPreviewUri,
        onSuccess = onSuccess,
        onError = { msg ->
            Toast.makeText(
                context,
                msg ?: context.getString(R.string.failed_to_create_skin),
                Toast.LENGTH_LONG
            ).show()
        }
    )
}
