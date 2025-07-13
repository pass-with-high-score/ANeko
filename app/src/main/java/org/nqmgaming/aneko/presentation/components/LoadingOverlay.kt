package org.nqmgaming.aneko.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme

@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    color: Color? = null,
    text: String? = null,
) {
    if (isLoading) {
        AlertDialog(
            containerColor = Color.Transparent,
            onDismissRequest = { },
            title = { },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = color ?: colorScheme.background,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = text ?: stringResource(R.string.txt_loading),
                            style = typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = color ?: colorScheme.background,
                        )
                    }
                }
            },
            confirmButton = { },
            dismissButton = { },
            modifier = modifier
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoadingOverlayPreview() {
    ANekoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            LoadingOverlay(isLoading = true)
        }
    }
}