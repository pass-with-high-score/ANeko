package com.nqmgaming.skin_sample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.nqmgaming.skin_sample.ui.theme.SkinTheme
import timber.log.Timber

class SkinActivity : ComponentActivity() {

    companion object {
        private const val ANEKO_PACKAGE = "org.nqmgaming.aneko"
        private const val ANEKO_ACTIVITY = "org.tamanegi.aneko.ANekoActivity"
        private val ANEKO_MARKET_URI = "market://search?q=$ANEKO_PACKAGE".toUri()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName(ANEKO_PACKAGE, ANEKO_ACTIVITY)
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e("Failed to open ANeko app: $e")
            setContent {
                SkinTheme(
                    dynamicColor = false
                ) {
                    InstallDialog()
                }
            }
        }
    }


    @Composable
    fun InstallDialog() {
        val context = LocalContext.current
        var openDialog by remember { mutableStateOf(true) }

        if (openDialog) {
            AlertDialog(
                onDismissRequest = {
                    openDialog = false
                    finish()
                },
                title = { Text(stringResource(R.string.app_name)) },
                text = { Text(stringResource(R.string.msg_no_package)) },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val marketIntent = Intent(Intent.ACTION_VIEW, ANEKO_MARKET_URI)
                                context.startActivity(marketIntent)
                            } catch (e: ActivityNotFoundException) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    R.string.msg_unexpected_err,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            openDialog = false
                            finish()
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }

}
