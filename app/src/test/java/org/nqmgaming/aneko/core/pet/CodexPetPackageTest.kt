package org.nqmgaming.aneko.core.pet

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class CodexPetPackageTest {
    @Test
    fun `uses declared spritesheet when it exists`() = withPackageDirectory { root ->
        val declared = root.resolve("custom-atlas.png").apply { writeText("declared") }
        root.resolve("spritesheet.webp").writeText("fallback")

        assertEquals(declared.canonicalFile, CodexPetPackage.resolveAtlasFile(root, declared))
    }

    @Test
    fun `falls back to the only supported spritesheet beside manifest`() =
        withPackageDirectory { root ->
            val declared = root.resolve("spritesheet.png")
            val fallback = root.resolve("spritesheet.webp").apply { writeText("fallback") }

            assertEquals(
                fallback.canonicalFile,
                CodexPetPackage.resolveAtlasFile(root, declared),
            )
        }

    @Test
    fun `does not guess when multiple fallback spritesheets exist`() =
        withPackageDirectory { root ->
            val declared = root.resolve("missing.webp")
            root.resolve("spritesheet.png").writeText("png")
            root.resolve("spritesheet.webp").writeText("webp")

            assertEquals(declared, CodexPetPackage.resolveAtlasFile(root, declared))
        }

    private fun withPackageDirectory(block: (java.io.File) -> Unit) {
        val root = Files.createTempDirectory("aneko-codex-pet-test").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }
}
