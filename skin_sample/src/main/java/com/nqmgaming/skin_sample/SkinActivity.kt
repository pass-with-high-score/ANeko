package com.nqmgaming.skin_sample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.nqmgaming.skin_sample.ui.theme.SkinTheme
import timber.log.Timber

class SkinActivity : ComponentActivity() {

    companion object {
        private const val ANEKO_PACKAGE = "org.nqmgaming.aneko"
        private const val ANEKO_ACTIVITY = "org.tamanegi.aneko.ANekoActivity"
        private const val ANEKO_MARKET_URI = "market://search?q=$ANEKO_PACKAGE"
        private const val GITHUB_SAMPLE_URI = "https://github.com/pass-with-high-score/ANeko"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isInstalled = isPackageInstalled()


        setContent {
            SkinTheme(dynamicColor = false) {
                val context = LocalContext.current
                val version = context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                ).versionName ?: stringResource(R.string.unknown_value)
                InstallScreen(
                    isInstalled,
                    onInstallClick = {

                        try {
                            val marketIntent = Intent(Intent.ACTION_VIEW, ANEKO_MARKET_URI.toUri())
                            startActivity(marketIntent)
                        } catch (e: ActivityNotFoundException) {
                            e.printStackTrace()
                            Toast.makeText(
                                this,
                                R.string.msg_unexpected_err,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        finish()
                    },
                    onExitClick = {
                        finish()
                    },
                    versionApp = version
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun InstallScreen(
        isInstalled: Boolean,
        onInstallClick: () -> Unit,
        onExitClick: () -> Unit,
        versionApp: String
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {},
                    actions = {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = GITHUB_SAMPLE_URI.toUri()
                                }
                                startActivity(intent)
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_github),
                                modifier = Modifier.size(24.dp),
                                contentDescription = null,
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                if (!isInstalled) {
                    Text(
                        text = stringResource(R.string.msg_no_package),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Install Button
                    Button(
                        onClick = onInstallClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.install))
                    }

                } else {
                    Text(
                        text = stringResource(R.string.msg_already_installed),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Open Button
                    Button(
                        onClick = {
                            try {
                                val intent = Intent().apply {
                                    action = Intent.ACTION_MAIN
                                    setClassName(ANEKO_PACKAGE, ANEKO_ACTIVITY)
                                }
                                startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                e.printStackTrace()
                                Toast.makeText(
                                    this@SkinActivity,
                                    R.string.msg_unexpected_err,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            finish()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.open))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exit Button
                OutlinedButton(
                    onClick = onExitClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(
                            R.string.app_version,
                            versionApp
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        }
    }

    private fun isPackageInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo(ANEKO_PACKAGE, 0)
            true
        } catch (e: Exception) {
            Timber.e(e, "Package not found")
            false
        }
    }

    @Preview
    @Composable
    private fun InstallScreenPreview() {
        SkinTheme(dynamicColor = false) {
            InstallScreen(
                isInstalled = false,
                onInstallClick = {},
                onExitClick = {},
                versionApp = "1"
            )
        }
    }
}
