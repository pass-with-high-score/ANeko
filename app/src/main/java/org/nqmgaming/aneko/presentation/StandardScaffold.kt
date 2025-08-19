package org.nqmgaming.aneko.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardScaffold(
    navController: NavController,
    showBottomBar: Boolean,
    items: List<BottomNavItem> = listOf(
        BottomNavItem.Home,
        BottomNavItem.Explore,
    ),
    content: @Composable (PaddingValues) -> Unit,
    viewModel: AnekoViewModel = hiltViewModel(),
) {
    val navigator = navController.rememberDestinationsNavigator()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Timber.e(e, "Failed to persist URI permission")
                }

                scope.launch(Dispatchers.IO) {
                    val pkg = viewModel.importSkinZipToAppStorage(context, uri)
                    withContext(Dispatchers.Main) {
                        if (pkg != null) {
                            Timber.d("Package name from skin XML: $pkg")
                        } else {
                            Timber.e("Failed to read package name from skin XML in ZIP")
                        }
                    }
                }
            }

        }
    )
    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    NavigationBar(
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(0.dp))
                            .background(colorScheme.surface),
                        containerColor = colorScheme.background,
                        contentColor = colorScheme.onBackground,
                        content = {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            items.forEach { item ->
                                val color by animateColorAsState(
                                    targetValue = if (currentDestination?.route?.contains(item.route) == true) {
                                        colorScheme.primary
                                    } else {
                                        colorScheme.onBackground
                                    },
                                    label = "color_anim"
                                )
                                val iconScale by animateFloatAsState(
                                    targetValue = if (currentDestination?.route?.contains(item.route) == true) {
                                        1.2f
                                    } else {
                                        1f
                                    },
                                    label = "scale_anim"
                                )
                                NavigationBarItem(
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colorScheme.primary,
                                        unselectedIconColor = colorScheme.onBackground,
                                        selectedTextColor = colorScheme.primary,
                                        unselectedTextColor = colorScheme.onBackground,
                                        indicatorColor = Color.Transparent,
                                    ),
                                    icon = {
                                        Icon(
                                            modifier = Modifier
                                                .size(25.dp)
                                                .scale(iconScale),
                                            painter = painterResource(id = item.icon),
                                            contentDescription = stringResource(item.title),
                                            tint = color
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(item.title),
                                            maxLines = 1,
                                            style = typography.bodySmall.copy(
                                                color = color,
                                                fontWeight = if (currentDestination?.route?.contains(
                                                        item.route
                                                    ) == true
                                                ) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                },
                                                fontSize = 10.sp
                                            ),
                                        )
                                    },
                                    alwaysShowLabel = true,
                                    selected = currentDestination?.route?.contains(item.route) == true,
                                    onClick = {
                                        navigator.navigate(item.direction) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // open file picker or download manager
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/zip",
                            "application/x-zip-compressed"
                        )
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null
                )
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}
