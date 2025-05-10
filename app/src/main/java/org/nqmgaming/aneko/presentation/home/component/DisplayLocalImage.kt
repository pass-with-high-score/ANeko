package org.nqmgaming.aneko.presentation.home.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import timber.log.Timber
import java.io.File

@Composable
fun DisplayLocalImage(drawableName: String, skinDir: File, modifier: Modifier = Modifier) {
    Timber.d("Skin Directory: ${skinDir.absolutePath}")
    Timber.d("Drawable Name: $drawableName")
    val imageFile = File(skinDir, drawableName)

    if (imageFile.exists()) {
        Timber.d("Image file exists: ${imageFile.absolutePath}")
        AsyncImage(
            model = imageFile,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Timber.e("Image file not found: ${imageFile.absolutePath}")
        Text("Image not found: $drawableName")
    }
}
