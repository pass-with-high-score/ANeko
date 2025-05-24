package org.nqmgaming.aneko.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.data.SkinInfo
import org.nqmgaming.aneko.data.skin.SkinConfig
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

suspend fun loadSkinList(context: Context): List<SkinInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val intent = Intent(AnimationService.ACTION_GET_SKIN)

    var skinList = pm.queryIntentActivities(intent, 0).map { resolveInfo ->
        val packageName = resolveInfo.activityInfo.packageName
        val versionName = try {
            pm.getPackageInfo(packageName, 0).versionName
                ?: context.getString(R.string.unknown_skin_version)
        } catch (_: PackageManager.NameNotFoundException) {
            context.getString(R.string.unknown_skin_version)
        }
        SkinInfo(
            icon = resolveInfo.loadIcon(pm),
            label = resolveInfo.loadLabel(pm).toString(),
            component = ComponentName(packageName, resolveInfo.activityInfo.name),
            versionName = versionName
        )
    }

    return@withContext skinList
}

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

