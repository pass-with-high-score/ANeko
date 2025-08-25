package org.nqmgaming.aneko.core.util

import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun zipDirectory(sourceDir: File, outputStream: OutputStream) {
    ZipOutputStream(outputStream).use { zos ->
        sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val entryName = file.relativeTo(sourceDir).path
            zos.putNextEntry(ZipEntry(entryName))
            FileInputStream(file).use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()
        }
    }
}