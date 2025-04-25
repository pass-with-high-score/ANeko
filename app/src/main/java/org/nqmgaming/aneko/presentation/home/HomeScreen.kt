package org.nqmgaming.aneko.presentation.home

import android.content.ComponentName
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.presentation.home.component.AddSkinCard
import org.nqmgaming.aneko.presentation.home.component.PowerToggleButton
import org.nqmgaming.aneko.presentation.home.component.SkinCard
import org.nqmgaming.aneko.presentation.setting.SettingsScreen
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme
import org.nqmgaming.aneko.util.createSkinList
import org.nqmgaming.aneko.util.openUrl


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = false,
    onChangeEnable: (Boolean) -> Unit = {},
    onSkinSelected: (ComponentName) -> Unit = {}
) {
    val context = LocalContext.current
    val skins = createSkinList(context)

    val initialSkinComponentString = context.getSharedPreferences(
        context.packageName + "_preferences",
        Context.MODE_PRIVATE
    ).getString(AnimationService.PREF_KEY_SKIN_COMPONENT, "")

    var selectedIndex by remember {
        mutableIntStateOf(
            skins.indexOfFirst { it.component.flattenToString() == initialSkinComponentString }
                .takeIf { it != -1 } ?: 0
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            itemsIndexed(skins) { index, skin ->
                SkinCard(
                    skin = skin,
                    isSelected = index == selectedIndex,
                    onClick = {
                        selectedIndex = index
                        onSkinSelected(skin.component)
                    }
                )
            }
            item {
                AddSkinCard(onClick = {
                    openUrl(context, context.getString(R.string.download_skins_link))
                })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.choose_skin),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            skins.forEachIndexed { index, _ ->
                val targetColor = if (index == selectedIndex) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                val color by animateColorAsState(targetColor)

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                        .padding(4.dp)
                )
                if (index != skins.lastIndex) Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PowerToggleButton(
                isEnabled = isEnabled,
                onChangeEnable = { checked ->
                    onChangeEnable(checked)
                }
            )
        }

        SettingsScreen()
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.make_with_love),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val version = context.packageManager.getPackageInfo(
                context.packageName,
                0
            ).versionName ?: stringResource(R.string.unknown_value)
            Text(
                text = stringResource(
                    R.string.app_version,
                    version
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ANekoTheme(
        dynamicColor = false
    ) {
        HomeScreen()
    }
}