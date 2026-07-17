package org.nqmgaming.aneko.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SafeZipExtractorTest {
    @Test
    fun `extracts nested files`() {
        val destination = Files.createTempDirectory("aneko-zip-test").toFile()
        try {
            val files = SafeZipExtractor.extract(
                ByteArrayInputStream(zipOf("pet/pet.json" to "{}")),
                destination,
            )
            assertEquals(1, files.size)
            assertEquals("{}", files.single().readText())
        } finally {
            destination.deleteRecursively()
        }
    }

    @Test
    fun `rejects zip slip entries`() {
        val destination = Files.createTempDirectory("aneko-zip-slip-test").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                SafeZipExtractor.extract(
                    ByteArrayInputStream(zipOf("../escaped.txt" to "nope")),
                    destination,
                )
            }
        } finally {
            destination.deleteRecursively()
        }
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, body) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
