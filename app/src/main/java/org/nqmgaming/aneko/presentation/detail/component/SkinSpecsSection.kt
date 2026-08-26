package org.nqmgaming.aneko.presentation.detail.component

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nqmgaming.aneko.R

@Composable
fun SkinSpecsSection(
    packageName: String,
    author: String,
    version: String,
    isCodex: Boolean,
    isBuiltin: Boolean,
    isInstalled: Boolean,
    isActive: Boolean,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.skin_specs_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            SpecItem(
                label = stringResource(R.string.package_skin_label, "").substringBefore(":").trim(),
                value = packageName
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            SpecItem(
                label = stringResource(R.string.label_author, "").substringBefore(":").trim(),
                value = author.ifBlank { stringResource(R.string.unknown_author) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            SpecItem(
                label = stringResource(R.string.skin_engine_type),
                value = if (isCodex) stringResource(R.string.skin_engine_codex)
                else stringResource(R.string.skin_engine_aneko)
            )

            if (version.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                SpecItem(
                    label = stringResource(R.string.version_skin_label, "").substringBefore(":").trim(),
                    value = version
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            SpecItem(
                label = stringResource(R.string.skin_format_label),
                value = when {
                    isBuiltin -> stringResource(R.string.skin_source_official)
                    isCodex -> stringResource(R.string.skin_source_petdex)
                    else -> stringResource(R.string.skin_source_community)
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            SpecItem(
                label = "Status",
                value = when {
                    isActive -> stringResource(R.string.skin_active_status)
                    isInstalled -> stringResource(R.string.skin_installed_status)
                    else -> stringResource(R.string.skin_not_installed_status)
                },
                highlight = isActive
            )
        }
    }
}

@Composable
private fun SpecItem(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
