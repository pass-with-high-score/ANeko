package org.nqmgaming.aneko.presentation.setting.component

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

@Composable
fun PreferenceSwitchItem(
    title: String,
    summary: String,
    icon: Int,
    key: String? = null,
    defaultValue: Boolean,
    prefs: SharedPreferences? = null,
    onClick: (() -> Unit)? = null,
    onSwitchClick: (() -> Unit)? = null,
) {
    var isChecked by remember {
        mutableStateOf(prefs?.getBoolean(key, defaultValue))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick != null) {
                onClick?.invoke()
            }
            .clip(MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                )
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = (if (onSwitchClick != null) defaultValue else isChecked) == true,
            onCheckedChange = {
                if (onSwitchClick != null) {
                    onSwitchClick()
                } else {
                    isChecked = it
                    prefs?.edit { putBoolean(key, it) }
                }
            }
        )
    }
}

