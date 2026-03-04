package org.nqmgaming.aneko.presentation.explore

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.launch
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.data.entity.SkinEntity
import org.nqmgaming.aneko.core.download.DownloadStatus
import org.nqmgaming.aneko.core.download.SkinDownloadQueue
import org.nqmgaming.aneko.core.util.extension.getStringResource
import org.nqmgaming.aneko.core.util.extension.openUrl
import org.nqmgaming.aneko.data.SkinCollection
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.components.LoadingOverlay
import org.nqmgaming.aneko.presentation.explore.component.ExploreItem
import org.nqmgaming.aneko.presentation.explore.component.InfoAlertDialog
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme
import timber.log.Timber

@Destination<RootGraph>
@Composable
fun ExploreSkinScreen(
    viewModel: AnekoViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isImporting by rememberSaveable { mutableStateOf(false) }

    SkinDownloadQueue.onImported = { _, uri ->
        scope.launch {
            try {
                isImporting = true
                val pkg = viewModel.importSkinFromUri(
                    context = context,
                    zipUri = uri.toUri(),
                    overwrite = true
                )
                Toast.makeText(
                    context,
                    if (pkg != null)
                        context.getStringResource(R.string.imported_skin_from_zip, pkg)
                    else
                        context.getStringResource(R.string.failed_to_import_skin_from_zip),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Timber.e(e)
                Toast.makeText(
                    context,
                    context.getStringResource(R.string.failed_to_import_skin, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isImporting = false
            }
        }
    }

    ExploreSkin(
        skinCollection = uiState.value.skinCollections,
        isLoading = uiState.value.isLoading,
        isRefreshing = uiState.value.isRefreshing,
        onRefresh = { viewModel.getSkinCollection(isRefresh = true) },
        skinsLocal = uiState.value.skins,
        onImportSkin = { uri ->
            if (isImporting) {
                Toast.makeText(
                    context,
                    context.getStringResource(R.string.importing_skin_please_wait),
                    Toast.LENGTH_SHORT
                ).show()
                return@ExploreSkin
            }
            scope.launch {
                try {
                    isImporting = true
                    val pkg = viewModel.importSkinFromUri(
                        context = context,
                        zipUri = uri,
                        overwrite = true
                    )
                    Toast.makeText(
                        context,
                        if (pkg != null)
                            context.getStringResource(R.string.imported_skin_from_zip, pkg)
                        else
                            context.getStringResource(R.string.failed_to_import_skin_from_zip),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Timber.e(e)
                    Toast.makeText(
                        context,
                        context.getStringResource(R.string.failed_to_import_skin, e.message ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    isImporting = false
                }
            }
        },
        onUninstall = { packageName ->
            val skin = uiState.value.skins.find { it.packageName == packageName }
            if (skin != null) {
                viewModel.onDeselectSkin(skin, context)
            }
        },
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
    onUninstall: (String) -> Unit = { },
) {
    val context = LocalContext.current
    val state = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    val statusMap by SkinDownloadQueue.status.collectAsState()

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

    // Tab definitions
    val tabTitles = listOf(
        stringResource(R.string.tab_built_in),
        stringResource(R.string.tab_community),
    )
    val tabIcons = listOf(
        Icons.Outlined.Inventory2,
        Icons.Outlined.People,
    )

    val pagerState = rememberPagerState(pageCount = { tabTitles.size })

    // Filter skins based on selected tab
    val builtInSkins = remember(skinCollection) {
        skinCollection?.filter { it.isBuiltIn } ?: emptyList()
    }
    val communitySkins = remember(skinCollection) {
        skinCollection?.filter { !it.isBuiltIn } ?: emptyList()
    }

    var isMenuExpanded by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.txt_explore),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black
                        ),
                    )
                },
                actions = {
                    IconButton(onClick = {
                        isSearchVisible = !isSearchVisible
                        if (!isSearchVisible) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        containerColor = MaterialTheme.colorScheme.surface,
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        shape = MaterialTheme.shapes.large,
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.what_is_this)) },
                            onClick = {
                                isMenuExpanded = false
                                isShowInfoDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.skin_share_dialog_title)) },
                            onClick = {
                                isMenuExpanded = false
                                context.openUrl(context.getStringResource(R.string.skin_share_url))
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.skin_builder_title)) },
                            onClick = {
                                isMenuExpanded = false
                                context.openUrl(context.getStringResource(R.string.skin_builder_url))
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.try_to_import_from_zip)) },
                            onClick = {
                                isMenuExpanded = false
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/x-zip-compressed"
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.FileOpen, contentDescription = null)
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (pagerState.currentPage == index)
                                    FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = tabIcons[index],
                                contentDescription = title,
                            )
                        },
                    )
                }
            }

            // Pager content
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
                modifier = Modifier.fillMaxSize()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val currentSkins = if (page == 0) builtInSkins else communitySkins

                    // Filter based on search query
                    val filteredSkins = remember(currentSkins, searchQuery) {
                        if (searchQuery.isBlank()) currentSkins
                        else currentSkins.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                                    (it.author?.contains(searchQuery, ignoreCase = true) == true)
                        }
                    }

                    if (skinCollection != null) {
                        val listState = rememberLazyListState()

                        LaunchedEffect(isSearchVisible) {
                            if (isSearchVisible) listState.animateScrollToItem(0)
                        }

                        LazyColumn(
                            state = listState,
                            modifier = modifier.fillMaxSize()
                        ) {
                            // Search field inside tab
                            item {
                                Column {
                                    AnimatedVisibility(visible = isSearchVisible) {
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            placeholder = { Text(stringResource(R.string.search_skin_placeholder)) },
                                            singleLine = true,
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = null
                                                )
                                            },
                                            trailingIcon = {
                                                if (searchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { searchQuery = "" }) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = null
                                                        )
                                                    }
                                                }
                                            },
                                            shape = MaterialTheme.shapes.extraLarge,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                                    alpha = 0.5f
                                                ),
                                            )
                                        )
                                    }
                                }
                            }

                            if (filteredSkins.isEmpty() && !isLoading) {
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
                                            modifier = Modifier.padding(16.dp),
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

                            items(
                                filteredSkins.size,
                                key = { filteredSkins[it].packageName }) { index ->
                                val collection = filteredSkins[index]
                                val st =
                                    statusMap[collection.packageName] ?: DownloadStatus.Idle
                                val queuePos =
                                    SkinDownloadQueue.queuePositionOf(collection.packageName)

                                ExploreItem(
                                    collection = collection,
                                    isInstalled = isInstalled(collection.packageName),
                                    st = st,
                                    queuePos = queuePos,
                                    onUninstall = if (isInstalled(collection.packageName)) {
                                        { onUninstall(collection.packageName) }
                                    } else null,
                                    localVersion = skinsLocal.find {
                                        it.packageName == collection.packageName
                                    }?.version ?: "",
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(if (isSearchVisible) 300.dp else 90.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    LoadingOverlay(isLoading && !isRefreshing)

    if (isShowInfoDialog) {
        InfoAlertDialog(
            onDismiss = { isShowInfoDialog = false }
        )
    }
}

@Preview
@Composable
private fun ExploreSkinPreview() {
    ANekoTheme { ExploreSkin() }
}
