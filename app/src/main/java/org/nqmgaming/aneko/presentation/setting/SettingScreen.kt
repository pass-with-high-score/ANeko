package org.nqmgaming.aneko.presentation.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.core.util.extension.getStringResource
import org.nqmgaming.aneko.core.util.extension.openUrl
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.setting.component.PreferenceContainer
import org.nqmgaming.aneko.presentation.setting.component.PreferenceItem
import org.nqmgaming.aneko.presentation.setting.component.SliderPreferenceItem
import org.nqmgaming.aneko.presentation.setting.component.SwitchPreferenceItem
import org.nqmgaming.aneko.presentation.setting.component.UpdateDialog
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme

@Composable
fun SettingsScreen(
    viewModel: AnekoViewModel? = null,
) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val uiState by viewModel?.uiState?.collectAsState()
        ?: return SettingsScreenContent()

    Column(modifier = Modifier.padding(16.dp)) {
        PreferenceContainer(
            title = R.string.motion_settings_title
        ) {
            SliderPreferenceItem(
                title = stringResource(R.string.motion_transparency_title),
                summary = stringResource(R.string.motion_transparency_summary),
                icon = R.drawable.ic_shape_exclude,
                entries = context.run { resources.getStringArray(R.array.pref_motion_transparency_entries) },
                entryValues = context.run { resources.getStringArray(R.array.pref_motion_transparency_entry_values) },
                key = AnimationService.PREF_KEY_TRANSPARENCY,
                defaultValue = "0.0",
                prefs = prefs
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.5f
                ),
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.motion_focus_title),
                summary = stringResource(R.string.motion_focus_summary),
                icon = R.drawable.ic_shape_exclude,
                key = AnimationService.PREF_KEY_FOCUS,
                defaultValue = false,
                prefs = prefs
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.5f
                ),
            )
            SliderPreferenceItem(
                title = stringResource(R.string.motion_speed_title),
                summary = stringResource(R.string.motion_speed_summary),
                icon = R.drawable.ic_speed,
                entries = context.run { resources.getStringArray(R.array.pref_motion_speed_entries) },
                entryValues = context.run { resources.getStringArray(R.array.pref_motion_speed_entry_values) },
                key = AnimationService.PREF_KEY_SPEED,
                defaultValue = "1.0",
                prefs = prefs
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.5f
                ),
            )
            SliderPreferenceItem(
                title = stringResource(R.string.motion_size_title),
                summary = stringResource(R.string.motion_size_summary),
                icon = R.drawable.ic_slide_size,
                entries = context.run { resources.getStringArray(R.array.pref_motion_size) },
                entryValues = context.run { resources.getStringArray(R.array.pref_motion_size_entry_values) },
                key = AnimationService.PREF_KEY_SIZE,
                defaultValue = "80",
                prefs = prefs
            )
        }

        Spacer(Modifier.height(16.dp))

        PreferenceContainer(
            title = R.string.community_title
        ) {
            PreferenceItem(
                title = stringResource(R.string.github_contribute_title),
                summary = stringResource(R.string.github_contribute_summary),
                icon = R.drawable.ic_github_mark,
                onClick = {
                    context.openUrl(context.getStringResource(R.string.github_uri))
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            )

            PreferenceItem(
                title = stringResource(R.string.website_title),
                summary = stringResource(R.string.website_summary),
                icon = R.drawable.ic_language,
                onClick = {
                    context.openUrl(context.getStringResource(R.string.aneko_url))
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            )

            PreferenceItem(
                title = stringResource(R.string.rate_app_title),
                summary = stringResource(R.string.rate_app_summary),
                icon = R.drawable.ic_star,
                onClick = {
                    context.openUrl(context.getStringResource(R.string.rate_app_url))
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            )

            PreferenceItem(
                title = stringResource(R.string.sponsor_title),
                summary = stringResource(R.string.sponsor_summary),
                icon = R.drawable.ic_heart,
                onClick = {
                    context.openUrl(context.getStringResource(R.string.sponsor_url))
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            )

            PreferenceItem(
                title = stringResource(R.string.check_for_update_title),
                summary = if (uiState.isCheckingUpdate)
                    stringResource(R.string.checking_for_update)
                else
                    stringResource(R.string.check_for_update_summary),
                icon = R.drawable.ic_update,
                onClick = {
                    if (!uiState.isCheckingUpdate) {
                        viewModel.checkForUpdate()
                    }
                }
            )
        }
    }

    // Update dialog
    uiState.updateInfo?.let { release ->
        UpdateDialog(
            release = release,
            onDismiss = { viewModel.dismissUpdate() }
        )
    }

    // Show "up to date" toast when check finishes with no update
    if (!uiState.isCheckingUpdate && uiState.updateInfo == null) {
        // This is handled reactively - toast shown from the click handler
    }
}

@Composable
private fun SettingsScreenContent() {
    // Fallback for preview without ViewModel
    Column(modifier = Modifier.padding(16.dp)) {}
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun SettingScreenPreview() {
    ANekoTheme(
        dynamicColor = false
    ) {
        SettingsScreen()
    }
}
