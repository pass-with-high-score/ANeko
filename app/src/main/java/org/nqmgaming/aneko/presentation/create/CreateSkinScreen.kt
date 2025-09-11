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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.presentation.AnekoViewModel
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.skin_name_label)) }
            )

            OutlinedTextField(
                value = packageName,
                onValueChange = { packageName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.skin_package_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )

            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.skin_author_label)) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    pickPreview.launch(arrayOf("image/*"))
                }) { Text(text = stringResource(R.string.pick_preview_image)) }

                Button(onClick = {
                    pickFrames.launch(arrayOf("image/*"))
                }) { Text(text = stringResource(R.string.add_frames_button)) }
            }

            Text(
                text = stringResource(
                    R.string.preview_selected_label,
                    (previewUri != null).toString()
                )
            )
            Text(text = stringResource(R.string.frames_selected_label, frameUris.size))

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
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
                },
                enabled = name.isNotBlank() && packageName.isNotBlank() && author.isNotBlank() && (previewUri != null) && frameUris.isNotEmpty()
            ) {
                Text(text = stringResource(R.string.create_skin_cta))
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

