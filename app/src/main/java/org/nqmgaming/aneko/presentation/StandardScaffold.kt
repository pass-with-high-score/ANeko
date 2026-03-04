package org.nqmgaming.aneko.presentation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nqmgaming.aneko.R
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardScaffold(
    navController: NavController,
    showBottomBar: Boolean,
    isAnimationEnabled: Boolean = false,
    onToggleAnimation: (Boolean) -> Unit = {},
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
    var isShowFAB by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(4.seconds)
        isShowFAB = true
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

                scope.launch(Dispatchers.IO) {
                    val pkg = viewModel.importSkinFromUri(context, uri)
                    withContext(Dispatchers.Main) {
                        if (pkg != null) {
                            Toast.makeText(
                                context,
                                "Imported skin from ZIP: $pkg",
                                Toast.LENGTH_SHORT
                            ).show()
                            Timber.d("Package name from skin XML: $pkg")
                        } else {
                            Timber.e("Failed to read package name from skin XML in ZIP")
                        }
                    }
                }
            }
        }
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.background(colorScheme.surface)
            ) {
                AppBottomBar(
                    items = items,
                    currentRoute = currentDestination?.route,
                    isAnimationEnabled = isAnimationEnabled,
                    onToggleAnimation = onToggleAnimation,
                    onItemClick = { item ->
                        val isCurrentDestOnBackStack =
                            currentDestination?.route?.contains(item.route) == true
                        if (isCurrentDestOnBackStack) {
                            navigator.popBackStack(item.direction, false)
                            return@AppBottomBar
                        }
                        navigator.navigate(item.direction) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (currentDestination?.route?.contains(BottomNavItem.Home.route) == true && isShowFAB) {
                FloatingActionButton(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    onClick = {
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
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun AppBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    isAnimationEnabled: Boolean,
    onToggleAnimation: (Boolean) -> Unit,
    onItemClick: (BottomNavItem) -> Unit,
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            containerColor = colorScheme.surface,
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(text = stringResource(R.string.overlay_permission_title)) },
            text = {
                Text(stringResource(R.string.overlay_permission_description))
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.allow_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }


    val powerBgColor by animateColorAsState(
        targetValue = if (isAnimationEnabled) colorScheme.primary else colorScheme.surfaceVariant,
        animationSpec = tween(400),
        label = "power_bg"
    )
    val powerIconColor by animateColorAsState(
        targetValue = if (isAnimationEnabled) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
        animationSpec = tween(400),
        label = "power_icon"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // The nav bar surface
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left nav items (before center)
                items.forEachIndexed { index, item ->
                    if (index == 1) {
                        // Spacer for center button
                        Spacer(modifier = Modifier.width(72.dp))
                    }

                    val isSelected = currentRoute?.contains(item.route) == true
                    val color by animateColorAsState(
                        targetValue = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
                        label = "nav_color_$index"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        label = "nav_scale_$index"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                onItemClick(item)
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .scale(iconScale),
                            painter = painterResource(id = item.icon),
                            contentDescription = stringResource(item.title),
                            tint = color
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(item.title),
                            maxLines = 1,
                            style = typography.labelSmall.copy(
                                color = color,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            ),
                        )
                    }
                }
            }
        }

        // Center power button — protruding above the nav bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-24).dp),
            contentAlignment = Alignment.Center
        ) {


            // Main button
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(
                        if (isAnimationEnabled) 8.dp else 0.dp,
                        CircleShape,
                        ambientColor = colorScheme.primary,
                        spotColor = colorScheme.primary
                    ),
                shape = CircleShape,
                color = powerBgColor,
                shadowElevation = 6.dp,
                onClick = {
                    if (!Settings.canDrawOverlays(context)) {
                        showPermissionDialog = true
                    } else {
                        onToggleAnimation(!isAnimationEnabled)
                    }
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = if (isAnimationEnabled) {
                            stringResource(R.string.power_on)
                        } else {
                            stringResource(R.string.power_off)
                        },
                        tint = powerIconColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
