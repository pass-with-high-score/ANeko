package org.nqmgaming.aneko.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.serialization.json.Json
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.data.skin.SkinConfig
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null, null, null
    )
    cursor?.use {
        if (it.moveToFirst()) {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex >= 0) {
                    return it.getString(columnIndex)
                }
            }
        }
    }
    return null
}

fun copyFileToAppDirectory(context: Context, uri: Uri, destFile: File) {
    val destDir = destFile.parentFile
    if (destDir != null) {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
    }

    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(destFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
    } catch (e: Exception) {
        Timber.e("Failed to copy file: $e")
    }
}

fun unzipFile(zipFile: File, destinationDir: File): File? {
    if (!destinationDir.exists()) {
        destinationDir.mkdirs()
    }

    var extractedDir: File? = null

    ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
        var entry: ZipEntry?
        while (zipInputStream.nextEntry.also { entry = it } != null) {
            val destFile = File(destinationDir, entry!!.name)

            if (entry.isDirectory) {
                destFile.mkdirs()
            } else {
                FileOutputStream(destFile).use { outputStream ->
                    zipInputStream.copyTo(outputStream)
                }
            }

            zipInputStream.closeEntry()

            if (entry.isDirectory && extractedDir == null) {
                extractedDir = destFile
            }
        }
    }

    return extractedDir
}


fun deleteFile(file: File) {
    if (file.exists()) {
        file.delete()
    }
}

fun getSkinConfigJsonFile(destinationDir: File): File? {
    val configFile = File(destinationDir, "skin_config.json")
    return if (configFile.exists()) {
        configFile
    } else {
        null
    }
}

fun readSkinConfigJson(file: File): SkinConfig? {
    return try {
        val jsonString = file.readText()
        val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }
        json.decodeFromString<SkinConfig>(jsonString)
    } catch (e: Exception) {
        Timber.e("Failed to read skin_config.json: $e")
        null
    }
}

fun saveSkin(context: Context, uri: Uri, skinConfig: SkinConfig?): Boolean {
    return try {
        val skinId = skinConfig?.info?.skinId ?: "default"
        val skinSaveDir = File(context.filesDir, "skins/$skinId")
        if (!skinSaveDir.exists()) {
            skinSaveDir.mkdirs()
        }

        if (uri.scheme == "content") {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputFile = File(skinSaveDir, "skin.zip")
            inputStream?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            unzipFile(outputFile, skinSaveDir)
            outputFile.delete()
        } else {
            val sourcePath = uri.path
            if (sourcePath != null) {
                val sourceDir = File(sourcePath)
                if (sourceDir.exists() && sourceDir.isDirectory) {
                    sourceDir.listFiles()?.forEach { file ->
                        file.copyTo(File(skinSaveDir, file.name), overwrite = true)
                    }
                }
            }
        }
        true
    } catch (e: Exception) {
        Timber.e("Failed to save skin: $e")
        false
    }
}

// Get all skin directories in the app's files directory return SkinConfig
fun getAllSkinConfigs(context: Context): List<SkinConfig> {
    val skinDir = File(context.filesDir, "skins")
    if (!skinDir.exists() || !skinDir.isDirectory) {
        return emptyList()
    }

    return skinDir.listFiles()?.mapNotNull { skinFolder ->
        val configFile = getSkinConfigJsonFile(skinFolder)
        if (configFile != null) {
            readSkinConfigJson(configFile)
        } else {
            null
        }
    } ?: emptyList()
}

// Delete a skin by its ID
fun deleteSkin(context: Context, skinId: String): Boolean {
    val skinDir = File(context.filesDir, "skins/$skinId")
    return if (skinDir.exists() && skinDir.isDirectory) {
        skinDir.deleteRecursively()
    } else {
        false
    }
}

fun shareSkin(context: Context, skinId: String) {
    val skinDir = File(context.filesDir, "skins/$skinId")
    if (skinDir.exists() && skinDir.isDirectory) {
        val zipFile = File(skinDir, "${skinId}.zip")
        if (!zipFile.exists()) {
            // Create a zip file from the skin directory
            zipFile.createNewFile()
            val zipOutputStream = java.util.zip.ZipOutputStream(FileOutputStream(zipFile))
            skinDir.listFiles()?.forEach { file ->
                val zipEntry = java.util.zip.ZipEntry(file.name)
                zipOutputStream.putNextEntry(zipEntry)
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(zipOutputStream)
                }
                zipOutputStream.closeEntry()
            }
            zipOutputStream.close()
        }

        // Use FileProvider to get content URI instead of file URI
        val contentUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )

        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
            type = "application/zip"
            // Grant temporary read permission to the content URI
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            android.content.Intent.createChooser(
                shareIntent,
                context.getString(R.string.share_apk_label, skinId)
            )
        )
    } else {
        Timber.e("Skin directory does not exist: $skinDir")
    }
}