package org.nqmgaming.aneko.presentation.home.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.data.Language
import org.nqmgaming.aneko.core.util.extension.changeLanguage
import org.nqmgaming.aneko.core.util.extension.getLanguageCode

@Composable
fun SelectLanguageDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val codes = LocalResources.current.getStringArray(R.array.language_codes)
    val names = LocalResources.current.getStringArray(R.array.language_names)
    val languages = codes.zip(names) { code, name -> Language(code, name) }

    val currentLanguage = remember {
        mutableStateOf(context.getLanguageCode())
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.choose_language_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(stringResource(R.string.contribute_to_translation))
                }
            }

        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                languages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (language.code == currentLanguage.value),
                                onClick = {
                                    currentLanguage.value = language.code
                                }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (language.code == currentLanguage.value),
                            onClick = {
                                currentLanguage.value = language.code
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    context.changeLanguage(currentLanguage.value)
                    onDismiss()
                }
            ) {
                Text("Ok")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}