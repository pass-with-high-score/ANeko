package org.nqmgaming.aneko.presentation.explore.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.util.openUrl

@Composable
fun InfoAlertDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.do_you_have_questions),
                    style = MaterialTheme.typography.headlineSmall
                )
                TextButton(
                    onClick = {
                        context.openUrl(
                            context.getString(R.string.skin_collection_link)
                        )
                    }
                ) {
                    Text(stringResource(R.string.open_the_skin_collection_configuration_file))
                }
            }
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.what_is_this),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    stringResource(R.string.what_is_this_answer),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.the_formats_and_structure_of_the_skins),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    stringResource(R.string.the_formats_and_structure_of_the_skins_answer),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Ok") }
        },
    )
}