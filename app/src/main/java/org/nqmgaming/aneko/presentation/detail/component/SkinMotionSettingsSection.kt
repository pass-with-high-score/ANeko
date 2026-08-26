package org.nqmgaming.aneko.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nqmgaming.aneko.R
import java.util.Locale

@Composable
fun SkinMotionSettingsSection(
    sizeValue: Float,
    sizeRange: ClosedFloatingPointRange<Float>,
    sizeSteps: Int,
    sizeDisplay: String,
    onSizeChange: (Float) -> Unit,
    onSizeChangeFinished: () -> Unit,

    transparencyValue: Float,
    transparencyRange: ClosedFloatingPointRange<Float>,
    transparencySteps: Int,
    transparencyDisplay: String,
    onTransparencyChange: (Float) -> Unit,
    onTransparencyChangeFinished: () -> Unit,

    speedValue: Float,
    speedRange: ClosedFloatingPointRange<Float>,
    speedSteps: Int,
    speedDisplay: String,
    onSpeedChange: (Float) -> Unit,
    onSpeedChangeFinished: () -> Unit,

    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.skin_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // ── Size Slider ──
            SettingSliderRow(
                label = stringResource(R.string.skin_size_label),
                displayValue = sizeDisplay,
                value = sizeValue,
                valueRange = sizeRange,
                steps = sizeSteps,
                accentColor = accentColor,
                onValueChange = onSizeChange,
                onValueChangeFinished = onSizeChangeFinished,
            )

            // ── Transparency Slider ──
            SettingSliderRow(
                label = stringResource(R.string.skin_transparency_label),
                displayValue = transparencyDisplay,
                value = transparencyValue,
                valueRange = transparencyRange,
                steps = transparencySteps,
                accentColor = accentColor,
                onValueChange = onTransparencyChange,
                onValueChangeFinished = onTransparencyChangeFinished,
            )

            // ── Speed Slider ──
            SettingSliderRow(
                label = stringResource(R.string.skin_speed_label),
                displayValue = speedDisplay,
                value = speedValue,
                valueRange = speedRange,
                steps = speedSteps,
                accentColor = accentColor,
                onValueChange = onSpeedChange,
                onValueChangeFinished = onSpeedChangeFinished,
            )
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    displayValue: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
            )
        )
    }
}
