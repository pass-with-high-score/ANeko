package org.nqmgaming.aneko.presentation.setting.component

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun SliderPreferenceItem(
    title: String,
    summary: String,
    icon: Int,
    entries: Array<String>,
    entryValues: Array<String>,
    key: String,
    defaultValue: String,
    prefs: SharedPreferences
) {
    val floatEntryValues = entryValues.mapNotNull { it.toFloatOrNull() }
    val valueRange = floatEntryValues.minOrNull()!!..floatEntryValues.maxOrNull()!!
    val steps = floatEntryValues.size - 2

    var sliderValue by remember {
        mutableFloatStateOf(
            prefs.getString(key, defaultValue)?.toFloatOrNull()?.coerceIn(valueRange)
                ?: floatEntryValues.first()
        )
    }

    val selectedIndex = floatEntryValues
        .indexOfFirst { it >= sliderValue }
        .let { if (it == -1) entries.lastIndex else it }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    val closest =
                        floatEntryValues.minByOrNull { fv -> kotlin.math.abs(fv - sliderValue) }
                            ?: floatEntryValues.first()
                    sliderValue = closest
                    prefs.edit { putString(key, closest.toString()) }
                },
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                    activeTickColor = MaterialTheme.colorScheme.primary,
                    inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = entries.getOrElse(selectedIndex) { stringResource(R.string.invalid_selection) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
