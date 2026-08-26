package org.nqmgaming.aneko.presentation.detail

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nqmgaming.aneko.core.pet.CodexPetPackage
import org.nqmgaming.aneko.core.util.SafeZipExtractor
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object SkinPreviewCache {

    /**
     * Returns the local directory (either in installed skins or cached preview directory).
     * If not available locally and downloadUrl is provided, downloads and extracts the skin package in background.
     */
    suspend fun getOrFetchDirectory(
        context: Context,
        packageName: String,
        downloadUrl: String?
    ): File? = withContext(Dispatchers.IO) {
        // 1. Check installed skins
        val installedDir = File(File(context.filesDir, "skins"), packageName)
        if (installedDir.exists() && installedDir.isDirectory && installedDir.listFiles()?.isNotEmpty() == true) {
            return@withContext installedDir
        }

        // 2. Check cached preview directory
        val root = File(context.cacheDir, "skin_previews")
        val previewDir = File(root, packageName)
        if (previewDir.exists() && previewDir.isDirectory && previewDir.listFiles()?.isNotEmpty() == true) {
            return@withContext previewDir
        }

        // 3. Download if downloadUrl is provided
        if (downloadUrl.isNullOrBlank()) return@withContext null

        try {
            previewDir.deleteRecursively()
            previewDir.mkdirs()

            val tempFile = File(context.cacheDir, "temp_preview_${packageName.replace('.', '_')}")
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.instanceFollowRedirects = true

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Try extracting as ZIP first
            var isExtracted = false
            try {
                tempFile.inputStream().use { input ->
                    SafeZipExtractor.extract(input, previewDir)
                }
                isExtracted = previewDir.listFiles()?.isNotEmpty() == true
            } catch (_: Exception) {
                isExtracted = false
            }

            // If not a valid ZIP, check if it's a standalone PNG/WebP atlas or image
            if (!isExtracted || previewDir.listFiles().isNullOrEmpty()) {
                val isImage = downloadUrl.endsWith(".png", true) ||
                        downloadUrl.endsWith(".webp", true) ||
                        tempFile.length() > 0
                if (isImage) {
                    val ext = if (downloadUrl.endsWith(".png", true)) "png" else "webp"
                    val destAtlas = File(previewDir, "spritesheet.$ext")
                    tempFile.copyTo(destAtlas, overwrite = true)
                    try {
                        val source = CodexPetPackage.fromStandalone(destAtlas, packageName)
                        File(previewDir, "pet.json").writeText(
                            """{"id":"${source.manifest.id}","displayName":"${source.manifest.displayName}","spriteVersionNumber":${source.version},"spritesheetPath":"${destAtlas.name}"}"""
                        )
                    } catch (_: Exception) {}
                }
            }

            tempFile.delete()
            if (previewDir.listFiles()?.isNotEmpty() == true) {
                return@withContext previewDir
            }
        } catch (e: Exception) {
            e.printStackTrace()
            previewDir.deleteRecursively()
        }

        return@withContext null
    }
}
