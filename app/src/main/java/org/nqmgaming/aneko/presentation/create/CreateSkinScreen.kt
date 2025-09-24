package org.nqmgaming.aneko.presentation.create

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.components.SectionCard
import coil.compose.AsyncImage
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CreateSkinScreen(
    navigator: DestinationsNavigator,
    viewModel: AnekoViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    val frameUris = remember { mutableStateListOf<Uri>() }
    val stateMapping = remember { mutableMapOf<String, MutableList<Pair<Uri, Int>>>() }
    var advancedMode by remember { mutableStateOf(false) }
    val allStates = listOf(
        "stop", "wait", "awake",
        "moveUp", "moveDown", "moveLeft", "moveRight",
        "moveUpLeft", "moveUpRight", "moveDownRight", "moveDownLeft",
        "wallUp", "wallDown", "wallLeft", "wallRight"
    )
    var currentState by remember { mutableStateOf(allStates.first()) }
    fun getStateList(key: String) = stateMapping.getOrPut(key) { mutableListOf() }
    val scope = rememberCoroutineScope()

    val pickPreview = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            persistUri(context, uri)
            previewUri = uri
        }
    }

    val pickFrames = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            persistUri(context, uri)
            frameUris.add(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.create_skin_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onCreateClicked(
                            context = context,
                            viewModel = viewModel,
                            name = name,
                            packageName = packageName,
                            author = author,
                            previewUri = previewUri,
                            frameUris = frameUris.toList(),
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.created_skin_success, it),
                                    Toast.LENGTH_SHORT
                                ).show()
                                navigator.popBackStack()
                            }
                        )
                    }) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    }
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
            // Live preview controls
            var frameMs by remember { mutableStateOf(250L) }
            var previewSize by remember { mutableStateOf(120.dp) }
            SectionCard(title = stringResource(R.string.section_details)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.skin_name_label)) })
                    OutlinedTextField(value = packageName, onValueChange = { packageName = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.skin_package_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                    OutlinedTextField(value = author, onValueChange = { author = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.skin_author_label)) })
                }
            }

            SectionCard(title = stringResource(R.string.section_assets)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { pickPreview.launch(arrayOf("image/*")) }) { Text(stringResource(R.string.pick_preview_image)) }
                        FilledTonalButton(onClick = { pickFrames.launch(arrayOf("image/*")) }) { Text(stringResource(R.string.add_frames_button)) }
                    }
                    Text(
                        text = stringResource(
                            R.string.preview_selected_label,
                            (previewUri != null).toString()
                        )
                    )
                    Text(text = stringResource(R.string.frames_selected_label, frameUris.size))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        frameUris.forEachIndexed { index, uri ->
                            ListItem(
                                headlineContent = { Text(uri.lastPathSegment ?: uri.toString()) },
                                leadingContent = { AsyncImage(model = uri, contentDescription = null) },
                                trailingContent = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { if (index > 0) { frameUris.removeAt(index); frameUris.add(index - 1, uri) } }) { Icon(Icons.Default.KeyboardArrowLeft, null) }
                                        IconButton(onClick = { if (index < frameUris.lastIndex) { frameUris.removeAt(index); frameUris.add(index + 1, uri) } }) { Icon(Icons.Default.KeyboardArrowRight, null) }
                                        IconButton(onClick = { frameUris.removeAt(index) }) { Icon(Icons.Default.Delete, null) }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(
                    R.string.preview_selected_label,
                    (previewUri != null).toString()
                )
            )
            Text(text = stringResource(R.string.frames_selected_label, frameUris.size))

            SectionCard(title = stringResource(R.string.section_preview)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FramesPreview(frames = frameUris.toList(), frameDurationMs = frameMs, size = previewSize)
                    Text(stringResource(R.string.label_speed))
                    Slider(value = frameMs.toFloat(), onValueChange = { frameMs = it.coerceIn(50f, 1000f).toLong() }, valueRange = 50f..1000f)
                    Text(stringResource(R.string.label_size))
                    Slider(value = previewSize.value, onValueChange = { previewSize = it.coerceIn(64f, 240f).dp }, valueRange = 64f..240f)
                }
            }

            // Reorder/remove frames list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                frameUris.forEachIndexed { index, uri ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "${index + 1}.")
                        IconButton(onClick = {
                            if (index > 0) {
                                frameUris.removeAt(index)
                                frameUris.add(index - 1, uri)
                            }
                        }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                        }
                        IconButton(onClick = {
                            if (index < frameUris.lastIndex) {
                                frameUris.removeAt(index)
                                frameUris.add(index + 1, uri)
                            }
                        }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                        }
                        IconButton(onClick = { frameUris.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                        Text(
                            text = uri.lastPathSegment ?: uri.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Advanced mapping section
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.advanced_mapping))
                Switch(checked = advancedMode, onCheckedChange = { advancedMode = it })
            }
            if (advancedMode) {
                var isMenuExpanded by remember { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { isMenuExpanded = true }) { Text(text = stringResource(R.string.choose_state)) }
                    DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                        allStates.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = {
                                currentState = s
                                isMenuExpanded = false
                            })
                        }
                    }
                    Button(onClick = {
                        // add all frames to current state with default durations
                        val list = getStateList(currentState)
                        frameUris.forEach { list.add(it to frameMs.toInt()) }
                    }) { Text(text = stringResource(R.string.add_all_frames_to_state)) }
                    Button(onClick = {
                        // clear state frames
                        getStateList(currentState).clear()
                    }) { Text(text = stringResource(R.string.clear_state_frames)) }
                }

                // State frames with per-frame duration and reordering
                val list = getStateList(currentState)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.forEachIndexed { idx, pair ->
                        val (u, d) = pair
                        var duration by remember(currentState, idx) { mutableStateOf(d.toString()) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "${idx + 1}.")
                            IconButton(onClick = {
                                if (idx > 0) {
                                    val item = list.removeAt(idx)
                                    list.add(idx - 1, item)
                                }
                            }) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null) }
                            IconButton(onClick = {
                                if (idx < list.lastIndex) {
                                    val item = list.removeAt(idx)
                                    list.add(idx + 1, item)
                                }
                            }) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
                            IconButton(onClick = { list.removeAt(idx) }) { Icon(Icons.Default.Delete, contentDescription = null) }
                            Text(text = u.lastPathSegment ?: u.toString(), modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = duration,
                                onValueChange = {
                                    val filtered = it.filter { ch -> ch.isDigit() }
                                    duration = filtered
                                    val v = filtered.toIntOrNull() ?: d
                                    list[idx] = u to v
                                },
                                label = { Text(stringResource(R.string.duration_ms)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        val pkg = viewModel.createPreviewSkin(
                            context = context,
                            name = name,
                            author = author,
                            previewUri = previewUri,
                            frameUrisPool = frameUris.toList(),
                            stateMapping = stateMapping.mapValues { it.value.toList() }
                        )
                        if (pkg != null) {
                            val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                            prefs.edit().putString(org.nqmgaming.aneko.core.service.AnimationService.PREF_KEY_SKIN_COMPONENT, pkg).apply()
                            prefs.edit().putBoolean(org.nqmgaming.aneko.core.service.AnimationService.PREF_KEY_VISIBLE, true).apply()
                            context.startService(
                                Intent(context, org.nqmgaming.aneko.core.service.AnimationService::class.java)
                                    .setAction(org.nqmgaming.aneko.core.service.AnimationService.ACTION_START)
                            )
                        } else {
                            Toast.makeText(context, context.getString(R.string.failed_to_create_skin), Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text(text = stringResource(R.string.preview_overlay_start)) }

                Button(
                onClick = {
                    if (advancedMode && stateMapping.values.any { it.isNotEmpty() }) {
                        scope.launch {
                            val pkg = viewModel.createSkinAdvanced(
                                context = context,
                                name = name,
                                packageName = packageName,
                                author = author,
                                previewUri = previewUri!!,
                                frameUrisPool = frameUris.toList(),
                                stateMapping = stateMapping.mapValues { it.value.toList() }
                            )
                            if (pkg != null) {
                                Toast.makeText(context, context.getString(R.string.created_skin_success, pkg), Toast.LENGTH_SHORT).show()
                                navigator.popBackStack()
                            } else {
                                Toast.makeText(context, context.getString(R.string.failed_to_create_skin), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        onCreateClicked(
                            context,
                            viewModel,
                            name,
                            packageName,
                            author,
                            previewUri,
                            frameUris.toList()
                        ) { createdPkg ->
                            Toast.makeText(
                                context,
                                context.getString(R.string.created_skin_success, createdPkg),
                                Toast.LENGTH_SHORT
                            ).show()
                            navigator.popBackStack()
                        }
                    }
                },
                enabled = name.isNotBlank() && packageName.isNotBlank() && author.isNotBlank() && (previewUri != null) && frameUris.isNotEmpty()
                ) {
                    Text(text = stringResource(R.string.create_skin_cta))
                }
                OutlinedButton(onClick = {
                    context.stopService(
                        Intent(context, org.nqmgaming.aneko.core.service.AnimationService::class.java)
                            .setAction(org.nqmgaming.aneko.core.service.AnimationService.ACTION_STOP)
                    )
                }) { Text(text = stringResource(R.string.preview_overlay_stop)) }
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

private fun onCreateClicked(
    context: Context,
    viewModel: AnekoViewModel,
    name: String,
    packageName: String,
    author: String,
    previewUri: Uri?,
    frameUris: List<Uri>,
    onSuccess: (String) -> Unit,
) {
    if (name.isBlank() || packageName.isBlank() || author.isBlank() || previewUri == null || frameUris.isEmpty()) {
        Toast.makeText(
            context,
            context.getString(R.string.create_skin_missing_fields),
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    viewModel.createSkin(
        context = context,
        name = name,
        packageName = packageName,
        author = author,
        previewUri = previewUri,
        frameUris = frameUris,
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
