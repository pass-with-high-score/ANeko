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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val backgroundColor = if (isEnabled) Color(0xFF4CAF50) else Color(0xFFB0BEC5)
    val iconColor = if (isEnabled) Color.White else Color.DarkGray
    val icon: ImageVector = Icons.Filled.PowerSettingsNew

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable {
                if (!Settings.canDrawOverlays(context)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                    context.startActivity(intent)
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