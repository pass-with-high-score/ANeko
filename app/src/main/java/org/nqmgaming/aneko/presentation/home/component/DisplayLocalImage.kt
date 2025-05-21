package org.nqmgaming.aneko.presentation.home.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File

@Composable
fun DisplayLocalImage(drawableName: String, skinDir: File, modifier: Modifier = Modifier) {
    val imageFile = File(skinDir, drawableName)

    if (imageFile.exists()) {
        AsyncImage(
            model = imageFile,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Text("Image not found: $drawableName")
    }
}
