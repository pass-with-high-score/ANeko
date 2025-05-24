package org.nqmgaming.aneko.presentation.home.component

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.nqmgaming.aneko.R

@Composable
fun PowerToggleButton(
    isEnabled: Boolean,
    onChangeEnable: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val backgroundColor =
        if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val iconColor =
        if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val icon: ImageVector = Icons.Filled.PowerSettingsNew

    var showPermissionDialog by remember { mutableStateOf(false) }

    if (showPermissionDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(text = stringResource(R.string.overlay_permission_title)) },
            text = {
                Text(
                    stringResource(R.string.overlay_permission_description)
                )
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

    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable {
                    if (!Settings.canDrawOverlays(context)) {
                        showPermissionDialog = true
                    } else {
                        onChangeEnable(!isEnabled)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isEnabled) stringResource(R.string.power_on) else stringResource(
                    R.string.power_off
                ),
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )
        }
    }

}
