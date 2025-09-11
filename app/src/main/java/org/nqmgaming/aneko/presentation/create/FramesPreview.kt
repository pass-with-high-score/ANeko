package org.nqmgaming.aneko.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun FramesPreview(
    frames: List<Any>,
    frameDurationMs: Long,
    size: Dp,
) {
    var index by remember(frames) { mutableIntStateOf(0) }

    LaunchedEffect(frames, frameDurationMs) {
        while (frames.isNotEmpty()) {
            delay(frameDurationMs.coerceAtLeast(50))
            index = (index + 1) % frames.size
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (frames.isNotEmpty()) {
            AsyncImage(
                model = frames[index],
                contentDescription = null,
                modifier = Modifier.size(size)
            )
        }
    }
}

