package org.nqmgaming.aneko.presentation.home.component

import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.nqmgaming.aneko.R
import timber.log.Timber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    onToggleTheme: () -> Unit,
    isDarkTheme: Boolean,
    onShowLanguageDialog: () -> Unit
) {
    val context = LocalContext.current
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black
                ),
            )
        },
        navigationIcon = {
            Row {
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = stringResource(R.string.toggle_theme_title)
                    )
                }
                IconButton(onClick = onShowLanguageDialog) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = stringResource(R.string.toggle_theme_title)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = {
                val uri = context.getString(R.string.telegram_group_link).toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_telegram),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = {
                val baseUri = context.getString(R.string.app_store_uri)
                val locale = Locale.getDefault().language
                val fullUri = "$baseUri&hl=$locale&gl=US"
                Timber.d("Share URI: $fullUri")

                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            fullUri
                        )
                    },
                    null
                ).also { context.startActivity(it) }
            }) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.share_title),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    )
}