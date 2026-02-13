package org.nqmgaming.aneko.presentation.home.component

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.util.extension.openUrl

@Composable
fun NotificationAlertDialog(
    onDismiss: () -> Unit,
) {
    val resource = LocalResources.current
    val context = LocalContext.current
    AlertDialog(
        containerColor = colorScheme.surface,
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.skin_share_dialog_title),
                style = typography.headlineSmall,
                color = colorScheme.primary
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = R.drawable.share_skin,
                    contentDescription = stringResource(R.string.skin_share_dialog_image_desc),
                    modifier = Modifier
                        .size(200.dp),

                    )
                Text(
                    stringResource(R.string.skin_share_dialog_body),
                    style = typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                context.openUrl(resource.getString(R.string.skin_share_url))
            }) {
                Text(stringResource(R.string.skin_share_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                Toast.makeText(
                    context,
                    resource.getString(R.string.skin_share_dialog_toast_hint),
                    Toast.LENGTH_LONG
                ).show()
            }) {
                Text(stringResource(R.string.skin_share_dialog_dismiss))
            }
        }
    )
}
