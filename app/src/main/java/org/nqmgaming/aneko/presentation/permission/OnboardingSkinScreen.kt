package org.nqmgaming.aneko.presentation.permission

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OnboardingSkinScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.download.DownloadStatus
import org.nqmgaming.aneko.core.download.SkinDownloadQueue
import org.nqmgaming.aneko.core.util.extension.getStringResource
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.components.LoadingOverlay
import org.nqmgaming.aneko.presentation.explore.component.ExploreItem
import timber.log.Timber

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSkinScreen(
    navigator: DestinationsNavigator,
    viewModel: AnekoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val skinCollection = uiState.skinCollections
    val skinsLocal = uiState.skins
    val isLoading = uiState.isLoading
    val isRefreshing = uiState.isRefreshing

    val state = rememberPullToRefreshState()
    val statusMap by SkinDownloadQueue.status.collectAsStateWithLifecycle()

    SkinDownloadQueue.onImported = { _, uri ->
        scope.launch {
            try {
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
            }
        }
    }

    fun isInstalled(packageName: String): Boolean {
        return skinsLocal.any { it.packageName == packageName }
    }

    fun navigateToHome() {
        viewModel.finishedSetup()
        navigator.navigate(HomeScreenDestination()) {
            popUpTo(OnboardingSkinScreenDestination) { inclusive = true }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.onboarding_skin_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black
                        ),
                    )
                },
                actions = {
                    TextButton(onClick = { navigateToHome() }) {
                        Text(stringResource(R.string.permission_onboarding_skip))
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
            ) {
                Button(
                    onClick = { navigateToHome() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skin_continue),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.getSkinCollection(isRefresh = true) },
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
            skinCollection?.let { collections ->
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (collections.isEmpty() && !isLoading) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.hi_there_is_no_skin_collection_for_now),
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    items(collections.size) { index ->
                        val collection = collections[index]
                        val st = statusMap[collection.packageName] ?: DownloadStatus.Idle
                        val queuePos = SkinDownloadQueue.queuePositionOf(collection.packageName)

                        ExploreItem(
                            collection = collection,
                            isInstalled = isInstalled(collection.packageName),
                            st = st,
                            queuePos = queuePos,
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    LoadingOverlay(isLoading && !isRefreshing)
}
