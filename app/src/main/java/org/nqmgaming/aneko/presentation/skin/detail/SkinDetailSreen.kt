package org.nqmgaming.aneko.presentation.skin.detail

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator
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
    viewModel: SkinDetailViewModel = hiltViewModel(),
    resultBackNavigator: ResultBackNavigator<Boolean>
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
            resultBackNavigator.navigateBack(true)
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
    var playSample by remember { mutableStateOf(false) }
    val context = LocalContext.current
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
                },
                actions = {
                    IconButton(onClick = {
                        skinConfig?.let { config ->
                            val skinDir = File(skinPath.toString())
                            val jsonFile = getSkinConfigJsonFile(skinDir)
                            if (jsonFile != null) {
                                try {
                                    skinPath?.let {
                                        val fileName = getFileNameFromUri(context, it)
                                        val destFile = File(context.filesDir, "skins/$fileName")
                                        copyFileToAppDirectory(context, it, destFile)
                                        unzipFile(destFile, skinDir)
                                        Toast.makeText(
                                            context,
                                            "Skin Config saved successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Timber.e("Failed to save skin config: $e")
                                }
                            }
                        }
                    }) {
                        // save button
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Info"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (skinConfig != null) {
                val motions = skinConfig.motionParams.motion

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        DisplayLocalImage(
                            drawableName = skinConfig.info.icon,
                            skinDir = File(skinPath.toString()),
                            modifier = Modifier
                                .size(100.dp)
                                .padding(8.dp)
                        )
                    }

                    Column {
                        Text(
                            skinConfig.info.name, style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(skinConfig.info.skinId, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            skinConfig.info.author,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                    }

                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)

                    ) {
                        IconButton(
                            onClick = {
                                playSample = !playSample
                            },
                        ) {
                            Icon(
                                imageVector = if (playSample) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play Sample",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),

                    ) {
                    items(motions) { motion ->
                        val frames = motion.actions.flattenToDrawableFrames()

                        if (frames.isNotEmpty()) {
                            var currentFrameIndex by remember { mutableIntStateOf(0) }

                            LaunchedEffect(playSample) {
                                while (playSample) {
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
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 8.dp,
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = motion.state,
                                        style = MaterialTheme.typography.labelSmall,
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
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No skin config found",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
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
