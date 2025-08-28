package org.nqmgaming.aneko.presentation.explore

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.data.entity.SkinEntity
import org.nqmgaming.aneko.data.SkinCollection
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.components.LoadingOverlay
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme
import org.nqmgaming.aneko.util.openUrl
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

@Destination<RootGraph>
@Composable
fun ExploreSkinScreen(
    viewModel: AnekoViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isImporting by rememberSaveable { mutableStateOf(false) }

    ExploreSkin(
        skinCollection = uiState.value.skinCollections,
        isLoading = uiState.value.isLoading,
        isRefreshing = uiState.value.isRefreshing,
        onRefresh = {
            viewModel.getSkinCollection(isRefresh = true)
        },
        skinsLocal = uiState.value.skins,
        onImportSkin = { uri ->
            if (isImporting) {
                Toast.makeText(
                    context,
                    context.getString(R.string.importing_skin_please_wait), Toast.LENGTH_SHORT
                ).show()
                return@ExploreSkin
            }
            try {
                scope.launch {
                    isImporting = true
                    val packageName = viewModel.importSkinFromUri(
                        context = context,
                        zipUri = uri,
                        overwrite = true,
                    )
                    if (packageName != null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.imported_skin_from_zip, packageName),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.failed_to_import_skin_from_zip),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    isImporting = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    context.getString(R.string.failed_to_import_skin, e.message), Toast.LENGTH_SHORT
                )
                    .show()
                isImporting = false
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreSkin(
    modifier: Modifier = Modifier,
    skinCollection: List<SkinCollection>? = null,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = { },
    skinsLocal: List<SkinEntity> = emptyList(),
    onImportSkin: (Uri) -> Unit = { _ -> },
) {
    val context = LocalContext.current
    val state = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    fun downloadSkinWithApi(context: Context, url: String, fileName: String) {
        scope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val file = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        "$fileName.zip"
                    )
                    FileOutputStream(file).use { output ->
                        response.body?.byteStream()?.copyTo(output)
                    }
                    onImportSkin(Uri.fromFile(file))
                }
            }
        }
    }

    fun isInstalled(packageName: String): Boolean {
        return skinsLocal.any { it.packageName == packageName }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri>? ->
            uris?.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Timber.e(e, "Failed to persist URI permission")
                }

                onImportSkin(uri)
            }
        }
    )

    var isShowInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.skin_collection),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black
                        ),
                    )
                },
                actions = {
                    IconButton(
                        onClick = { isShowInfoDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = state,
            indicator = {
                Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.surface,
                    color = MaterialTheme.colorScheme.primary,
                    state = state
                )
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            skinCollection?.let {
                LazyColumn(
                    modifier = modifier,
                ) {
                    if (it.isEmpty() && !isLoading) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.hi_there_is_no_skin_collection_for_now),
                                    modifier = Modifier
                                        .padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Button(onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf(
                                            "application/zip",
                                            "application/x-zip-compressed"
                                        )
                                    )
                                }) {
                                    Text(
                                        text = stringResource(R.string.try_to_import_from_zip),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                    items(it.size) { index ->
                        val collection = it[index]
                        Column {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(collection.image)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                )
                                Spacer(modifier = Modifier.size(16.dp))
                                Column(
                                    modifier = Modifier.weight(4f)
                                ) {
                                    Text(
                                        text = collection.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = collection.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = collection.author
                                            ?: stringResource(R.string.unknown_author),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        downloadSkinWithApi(
                                            context = context,
                                            url = collection.url,
                                            fileName = collection.name,
                                        )

                                    },
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                ) {

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = stringResource(R.string.download),
                                        )
                                        if (isInstalled(collection.packageName)) {
                                            Text(
                                                text = stringResource(R.string.overwrite),
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )

                                        } else {
                                            Text(
                                                text = stringResource(R.string.download),
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }

                                    }

                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            } ?: run {
            }
        }

    }

    LoadingOverlay(isLoading && !isRefreshing)


    if (isShowInfoDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = {
                isShowInfoDialog = false
            },
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.do_you_have_questions),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    TextButton(
                        onClick = {
                            openUrl(context, context.getString(R.string.skin_collection_link))
                        }
                    ) {
                        Text(stringResource(R.string.open_the_skin_collection_configuration_file))
                    }
                }

            },
            text = {
                Column {
                    Text(
                        stringResource(R.string.what_is_this),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        stringResource(R.string.what_is_this_answer),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.the_formats_and_structure_of_the_skins),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        stringResource(R.string.the_formats_and_structure_of_the_skins_answer),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowInfoDialog = false
                    }
                ) {
                    Text("Ok")
                }
            },
        )
    }
}

@Preview
@Composable
private fun ExploreSkinPreview() {
    ANekoTheme {
        ExploreSkin()
    }
}