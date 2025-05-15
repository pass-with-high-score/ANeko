package org.nqmgaming.aneko.presentation.home.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.nqmgaming.aneko.R

@Composable
fun SkinSourceDialog(
    onDismiss: () -> Unit,
    onDownloadCollection: () -> Unit,
    onSearchPlayStore: () -> Unit
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.choose_skin_source_title))
        },
        text = {
            Column {
                Text(stringResource(R.string.skin_source_option_download_from_collection))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.skin_source_option_download_from_store))
            }
        },
        confirmButton = {
            TextButton(onClick = onDownloadCollection) {
                Text(stringResource(R.string.download_collection_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onSearchPlayStore) {
                Text(stringResource(R.string.search_on_play_store_label))
            }
        }
    )
}
