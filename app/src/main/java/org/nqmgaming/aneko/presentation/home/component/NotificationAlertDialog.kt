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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.util.extension.getStringResource
import org.nqmgaming.aneko.core.util.extension.openUrl

@Composable
fun NotificationAlertDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        containerColor = colorScheme.surface,
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Custom skins made easy with ANeko Builder",
                style = typography.headlineSmall
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = R.drawable.skin_builder,
                    contentDescription = "Skin builder preview",
                    modifier = Modifier
                        .size(200.dp),

                    )
                Text(
                    "Thank you for installing ANeko! ANeko Builder is a new web tool designed to make creating and customizing skins simple and fun.",
                    style = typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                context.openUrl(context.getStringResource(R.string.skin_builder_url))
            }) {
                Text("Take me there")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                Toast.makeText(
                    context,
                    "You can access ANeko Builder later from the Explore tab.",
                    Toast.LENGTH_LONG
                ).show()
            }) {
                Text("Maybe Later")
            }
        }
    )
}