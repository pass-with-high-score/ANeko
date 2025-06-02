package org.nqmgaming.aneko.presentation.home.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

@Composable
fun DisplayLocalImage(
    drawableName: String,
    skinDir: File,
    modifier: Modifier = Modifier
) {
    val imageFile = File(skinDir, drawableName)

    if (imageFile.exists()) {
        val bitmap = remember(imageFile.path) {
            BitmapFactory.decodeFile(imageFile.absolutePath)?.asImageBitmap()
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Text("Failed to load image: $drawableName")
        }
    } else {
        Text("Image not found: $drawableName")
    }
}
