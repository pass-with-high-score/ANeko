package org.nqmgaming.aneko.presentation.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import org.nqmgaming.aneko.data.SkinCollection
import org.nqmgaming.aneko.presentation.components.LoadingOverlay
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme

@Destination<RootGraph>
@Composable
fun ExploreSkinScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsState()
    ExploreSkin(
        skinCollection = uiState.value.skinCollections,
        isLoading = uiState.value.isLoading,
        isRefreshing = uiState.value.isRefreshing,
        onRefresh = {
            viewModel.getSkinCollection(isRefresh = true)
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
    onRefresh: () -> Unit = { }
) {
    val context = LocalContext.current
    val state = rememberPullToRefreshState()
    Scaffold { innerPadding ->
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
                    items(it.size) { index ->
                        val collection = it[index]
                        Column {
                            Row(modifier = Modifier.padding(16.dp)) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(collection.image)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(56.dp)
                                )
                                Spacer(modifier = Modifier.size(16.dp))
                                Column {
                                    Text(
                                        text = collection.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = collection.version,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = collection.packageName,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } ?: run {
            }
        }

    }

    LoadingOverlay(isLoading)
}

@Preview
@Composable
private fun ExploreSkinPreview() {
    ANekoTheme {
        ExploreSkin()
    }
}