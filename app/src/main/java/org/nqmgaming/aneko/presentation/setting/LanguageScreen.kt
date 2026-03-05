package org.nqmgaming.aneko.presentation.setting

import android.app.Activity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.util.LocaleHelper
import org.nqmgaming.aneko.core.util.extension.changeLanguage
import org.nqmgaming.aneko.data.Language

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val codes = LocalResources.current.getStringArray(R.array.language_codes)
    val names = LocalResources.current.getStringArray(R.array.language_names)
    val languages = remember { codes.zip(names) { code, name -> Language(code, name) } }

    val currentLanguage = remember {
        mutableStateOf(LocaleHelper.getLocale(context))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.choose_language_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            (context as Activity).changeLanguage(currentLanguage.value)
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(languages) { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (language.code == currentLanguage.value),
                            onClick = {
                                currentLanguage.value = language.code
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (language.code == currentLanguage.value),
                        onClick = {
                            currentLanguage.value = language.code
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = language.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
