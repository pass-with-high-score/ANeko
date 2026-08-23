package org.nqmgaming.aneko.core.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object SafeZipExtractor {
    private const val MAX_ENTRIES = 512
    private const val MAX_TOTAL_BYTES = 128L * 1024L * 1024L

    fun extract(input: InputStream, destination: File): List<File> {
        val root = destination.canonicalFile.apply { mkdirs() }
        val extracted = mutableListOf<File>()
        var entries = 0
        var totalBytes = 0L

        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries += 1
                require(entries <= MAX_ENTRIES) { "ZIP contains too many entries" }

                val ignored = entry.name.startsWith("__MACOSX") ||
                    entry.name.endsWith(".DS_Store")
                if (!entry.isDirectory && !ignored) {
                    val output = File(root, entry.name).canonicalFile
                    require(output.path.startsWith(root.path + File.separator)) {
                        "ZIP entry escapes destination: ${entry.name}"
                    }
                    output.parentFile?.mkdirs()
                    BufferedOutputStream(output.outputStream()).use { stream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            totalBytes += count
                            require(totalBytes <= MAX_TOTAL_BYTES) {
                                "ZIP uncompressed content exceeds 128 MiB"
                            }
                            stream.write(buffer, 0, count)
                        }
                    }
                    extracted += output
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return extracted
    }
}
