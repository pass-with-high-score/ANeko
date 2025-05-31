package org.nqmgaming.aneko.presentation.home.component

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.data.skin.SkinConfig
import org.nqmgaming.aneko.data.skin.SkinInfo
import org.nqmgaming.aneko.presentation.setting.SettingsScreen
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme
import org.nqmgaming.aneko.util.deleteSkin
import org.nqmgaming.aneko.util.getAllSkinConfigs
import org.nqmgaming.aneko.util.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = false,
    onChangeEnable: (Boolean) -> Unit = {},
    onSkinSelected: (SkinInfo) -> Unit = {},
) {
    val context = LocalContext.current
    var refreshing by remember { mutableStateOf(true) }
    var skinList by remember { mutableStateOf<List<SkinConfig>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val state = rememberPullToRefreshState()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(refreshing) {
        if (refreshing) {
            val skins = getAllSkinConfigs(context)
            skinList = skins

            // Set selected index after skinList is loaded
            val initialSkinComponentString = context.getSharedPreferences(
                context.packageName + "_preferences",
                Context.MODE_PRIVATE
            ).getString(AnimationService.PREF_KEY_SKIN_COMPONENT, "")

            selectedIndex =
                skins.indexOfFirst { it.info.skinId == initialSkinComponentString }
                    .takeIf { it != -1 } ?: 0

            refreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
        },
        state = state,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = refreshing,
                containerColor = MaterialTheme.colorScheme.onPrimary,
                color = MaterialTheme.colorScheme.primary,
                state = state
            )
        },
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(
                targetState = skinList.isNotEmpty()
            ) { isReady ->
                if (isReady) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        itemsIndexed(skinList) { index, skin ->
                            SkinCard(
                                skin = skin,
                                isSelected = index == selectedIndex,
                                onSkinSelected = {
                                    selectedIndex = index
                                    onSkinSelected(skin.info)
                                    refreshing = true
                                },
                                onRequestDeleteSkin = {
                                    deleteSkin(context, skin.info.skinId)
                                }
                            )
                        }
                        item {
                            AddSkinCard(onClick = {
                                showDialog = true
                            })
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.loading_skins_label))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.choose_skin),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PowerToggleButton(
                    isEnabled = isEnabled,
                    onChangeEnable = { checked ->
                        onChangeEnable(checked)
                    }
                )
            }

            SettingsScreen(
                onBrowseSkins = {
                    showDialog = true
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.make_with_love),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val version = context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                ).versionName ?: stringResource(R.string.unknown_value)
                Text(
                    text = stringResource(
                        R.string.app_version,
                        version
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDialog) {
        SkinSourceDialog(
            onDismiss = { showDialog = false },
            onDownloadCollection = {
                showDialog = false
                openUrl(context, context.getString(R.string.download_skins_link))
            },
            onSearchPlayStore = {
                showDialog = false
                openUrl(context, context.getString(R.string.search_app))
            }
        )
    }
}


@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ANekoTheme(
        dynamicColor = false
    ) {
        HomeContent()
    }
}