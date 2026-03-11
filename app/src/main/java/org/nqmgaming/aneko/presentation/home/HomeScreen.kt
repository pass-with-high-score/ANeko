package org.nqmgaming.aneko.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.LanguageScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThemeScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.home.component.HomeAppBar
import org.nqmgaming.aneko.presentation.home.component.HomeContent
import org.nqmgaming.aneko.presentation.home.component.NotificationAlertDialog
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.data.SkinCollection
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxWidth
import com.ramcosta.composedestinations.generated.destinations.ExploreSkinScreenDestination

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    viewModel: AnekoViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val isFirstLaunch = viewModel.isFirstLaunch.collectAsStateWithLifecycle().value
    var isShowingDialog by rememberSaveable { mutableStateOf(isFirstLaunch) }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeAppBar(
                onShowLanguageScreen = {
                    navigator.navigate(LanguageScreenDestination())
                },
                onShowThemeScreen = {
                    navigator.navigate(ThemeScreenDestination())
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            HomeContent(
                modifier = Modifier
                    .padding(innerPadding),
                onToggleSkin = { packageName ->
                    viewModel.onToggleSkin(packageName, context)
                },
                skins = uiState.value.skins,
                onRequestDeleteSkin = {
                    viewModel.onDeselectSkin(it, context)
                },
            )
        }

        if (isFirstLaunch && isShowingDialog) {
            // show welcome dialog
            NotificationAlertDialog(
                onDismiss = {
                    isShowingDialog = false
                    viewModel.setFirstLaunchDone()
                }
            )
        } else if (uiState.value.newAvailableSkins.isNotEmpty()) {
            NewSkinAlertDialog(
                skins = uiState.value.newAvailableSkins,
                onDismiss = {
                    viewModel.dismissNewSkinDialog()
                },
                onNavigate = {
                    viewModel.dismissNewSkinDialog()
                    navigator.navigate(ExploreSkinScreenDestination())
                }
            )
        }
    }
}

@Composable
fun NewSkinAlertDialog(
    skins: List<SkinCollection>,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit
) {
    if (skins.isEmpty()) return
    val titleRes =
        if (skins.size == 1) R.string.new_skin_available_title else R.string.new_skins_available_title

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            LazyColumn {
                items(skins) { skin ->
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        AsyncImage(
                            model = skin.image,
                            contentDescription = skin.name,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = skin.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNavigate) {
                Text(stringResource(R.string.new_skin_dialog_navigate_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.new_skin_dialog_dismiss_button))
            }
        }
    )
}
