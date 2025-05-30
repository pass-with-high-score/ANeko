package org.nqmgaming.aneko.presentation.skin.detail

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import org.nqmgaming.aneko.data.skin.SkinConfig
import org.nqmgaming.aneko.data.skin.flattenToDrawableFrames
import org.nqmgaming.aneko.presentation.home.component.DisplayLocalImage
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme
import org.nqmgaming.aneko.util.copyFileToAppDirectory
import org.nqmgaming.aneko.util.deleteFile
import org.nqmgaming.aneko.util.getFileNameFromUri
import org.nqmgaming.aneko.util.getSkinConfigJsonFile
import org.nqmgaming.aneko.util.readSkinConfigJson
import org.nqmgaming.aneko.util.unzipFile
import timber.log.Timber
import java.io.File

@Destination<RootGraph>(
    navArgs = SkinDetailArgs::class,
)
@Composable
fun SkinDetailScreen(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    viewModel: SkinDetailViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(key1 = uiState.value.skinPath) {
        val fileName = getFileNameFromUri(context, uiState.value.skinPath)
        if (fileName != null) {
            val destFile = File(context.filesDir, "skins/$fileName")

            copyFileToAppDirectory(context, uiState.value.skinPath, destFile)
            val destinationDir = File(context.filesDir, "skins/unzipped")
            try {
                val extractedDir = unzipFile(destFile, destinationDir)
                viewModel.onEvent(SkinDetailUiAction.UpdateSkinPath(extractedDir?.path?.toUri()))
                extractedDir?.let { skinDir ->
                    val configFile = getSkinConfigJsonFile(skinDir)
                    configFile?.let { config ->
                        val skinConfig = readSkinConfigJson(config)
                        Timber.d("Skin Config: $skinConfig")
                        viewModel.onEvent(SkinDetailUiAction.UpdateSkinConfig(skinConfig = skinConfig))
                    }
                }
                deleteFile(destFile)
            } catch (e: Exception) {
                Timber.e("Failed to unzip file: $e")
            }
        }
    }
    SkinDetail(
        modifier,
        onNavigateBack = {
            navigator.navigateUp()
        },
        skinConfig = uiState.value.skinConfig,
        skinPath = uiState.value.skinPath,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinDetail(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    skinConfig: SkinConfig? = null,
    skinPath: Uri? = null
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Skin Detail",
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

        if (skinConfig != null) {
            val motions = skinConfig.motionParams.motion

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(innerPadding),
            ) {
                items(motions) { motion ->
                    val frames = motion.actions.flattenToDrawableFrames()

                    if (frames.isNotEmpty()) {
                        var currentFrameIndex by remember { mutableIntStateOf(0) }

                        LaunchedEffect(motion.state) {
                            while (true) {
                                val duration = frames[currentFrameIndex].durationMillis.toLong()
                                delay(duration)
                                currentFrameIndex = (currentFrameIndex + 1) % frames.size
                            }
                        }

                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 4.dp,
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = motion.state,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .align(Alignment.CenterHorizontally)
                                )

                                DisplayLocalImage(
                                    drawableName = frames[currentFrameIndex].drawableName,
                                    skinDir = File(skinPath.toString()),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .padding(bottom = 8.dp)
                                )

                                Text(
                                    text = "Duration: ${frames[currentFrameIndex].durationMillis} ms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }


    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun SkinDetailPreview() {
    ANekoTheme(dynamicColor = false) {
        SkinDetail()
    }
}
