package org.nqmgaming.aneko.presentation.home.component

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import timber.log.Timber
import java.io.File

@Composable
fun DisplayLocalImage(drawableName: String, skinDir: File, modifier: Modifier = Modifier) {
    Timber.d("Skin Directory: ${skinDir.absolutePath}")
    Timber.d("Drawable Name: $drawableName")
    val imageFile = File(skinDir, drawableName) // Tạo đường dẫn từ skinDir và tên ảnh

    if (imageFile.exists()) {
        Timber.d("Image file exists: ${imageFile.absolutePath}")
        val painter = rememberAsyncImagePainter(imageFile) // Dùng Coil để load ảnh từ file
        Image(
            painter = painter,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop // Bạn có thể thay đổi contentScale nếu cần
        )
    } else {
        Timber.e("Image file not found: ${imageFile.absolutePath}")
        Text("Image not found: $drawableName")
    }
}
