package org.nqmgaming.aneko.presentation.skin.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme

@Destination<RootGraph>
@Composable
fun EditSkinScreen(modifier: Modifier = Modifier, navigator: DestinationsNavigator) {
    EditSkin(
        modifier,
        onNavigateBack = {
            navigator.navigateUp()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSkin(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Skin",
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Text(
                "EditSkin"
            )
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun EditSkinPreview() {
    ANekoTheme(dynamicColor = false) {
        EditSkin()
    }
}
