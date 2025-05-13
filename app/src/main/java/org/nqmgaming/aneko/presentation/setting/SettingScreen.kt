package org.nqmgaming.aneko.presentation.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.presentation.setting.component.ListPreferenceItem
import org.nqmgaming.aneko.presentation.setting.component.PreferenceContainer
import org.nqmgaming.aneko.presentation.setting.component.PreferenceItem
import org.nqmgaming.aneko.presentation.setting.component.PreferenceSwitchItem
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme
import org.nqmgaming.aneko.util.openUrl

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    Column(modifier = Modifier.padding(16.dp)) {
        PreferenceContainer(
            title = R.string.motion_settings_title
        ) {
            ListPreferenceItem(
                title = stringResource(R.string.motion_transparency_title),
                summary = stringResource(R.string.motion_transparency_summary),
                icon = R.drawable.ic_shape_exclude,
                entries = context.resources.getStringArray(R.array.pref_motion_transparency_entries),
                entryValues = context.resources.getStringArray(R.array.pref_motion_transparency_entry_values),
                key = AnimationService.PREF_KEY_TRANSPARENCY,
                defaultValue = "0.0",
                prefs = prefs
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.5f
                ),
            )
            ListPreferenceItem(
                title = stringResource(R.string.motion_speed_title),
                summary = stringResource(R.string.motion_speed_summary),
                icon = R.drawable.ic_speed,
                entries = context.resources.getStringArray(R.array.pref_motion_speed_entries),
                entryValues = context.resources.getStringArray(R.array.pref_motion_speed_entry_values),
                key = AnimationService.PREF_KEY_SPEED,
                defaultValue = "1.0",
                prefs = prefs
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.5f
                ),
            )
            ListPreferenceItem(
                title = stringResource(R.string.motion_size_title),
                summary = stringResource(R.string.motion_size_summary),
                icon = R.drawable.ic_slide_size,
                entries = context.resources.getStringArray(R.array.pref_motion_size),
                entryValues = context.resources.getStringArray(R.array.pref_motion_size_entry_values),
                key = AnimationService.PREF_KEY_SIZE,
                defaultValue = "80",
                prefs = prefs
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.5f
                ),
            )

            PreferenceSwitchItem(
                title = stringResource(R.string.motion_keep_alive_title),
                summary = stringResource(R.string.motion_keep_alive_summary),
                icon = R.drawable.ic_auto_renew,
                key = AnimationService.PREF_KEY_KEEP_ALIVE,
                defaultValue = true,
                prefs = prefs
            )
        }

        Spacer(Modifier.height(16.dp))

        PreferenceContainer(
            title = R.string.customization_title
        ) {
            PreferenceItem(
                title = stringResource(R.string.browse_skins_title),
                summary = stringResource(R.string.browse_skins_summary),
                icon = R.drawable.ic_slide_multiple_search,
                onClick = {
                    openUrl(context, context.getString(R.string.skin_search_uri))
                }
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
                    openUrl(context, context.getString(R.string.github_uri))
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            )

            PreferenceItem(
                title = stringResource(R.string.rate_app_title),
                summary = stringResource(R.string.rate_app_summary),
                icon = R.drawable.ic_star, // Or any icon you have
                onClick = {
                    openUrl(context, context.getString(R.string.rate_app_url))
                }
            )
        }

    }
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


