package org.nqmgaming.aneko.presentation.setting.component

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.nqmgaming.aneko.R

@Composable
fun ListPreferenceItem(
    title: String,
    summary: String,
    icon: Int,
    entries: Array<String>,
    entryValues: Array<String>,
    key: String,
    defaultValue: String,
    prefs: SharedPreferences
) {
    var selectedValue by remember {
        mutableStateOf(
            prefs.getString(key, defaultValue)?.takeIf { it in entryValues } ?: defaultValue
        )
    }
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { showDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    summary, style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = entries.getOrElse(entryValues.indexOf(selectedValue)) { stringResource(R.string.invalid_selection) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Dialog to select an option
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.select_prompt, title)) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    entries.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedValue = entryValues[index]
                                    prefs.edit { putString(key, selectedValue) }
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry, style = MaterialTheme.typography.bodyMedium)
                            RadioButton(
                                selected = selectedValue == entryValues[index],
                                onClick = {
                                    selectedValue = entryValues[index]
                                    prefs.edit { putString(key, selectedValue) }
                                    showDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}