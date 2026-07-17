package org.nqmgaming.aneko.core.pet

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.nqmgaming.aneko.core.data.entity.SkinEntity
import java.io.File

@Serializable
data class CodexPetManifest(
    val id: String,
    val displayName: String,
    val description: String = "",
    val spriteVersionNumber: Int = 1,
    val spritesheetPath: String = "spritesheet.webp",
)

data class CodexPetSource(
    val manifest: CodexPetManifest,
    val manifestFile: File?,
    val atlasFile: File,
    val version: Int,
) {
    val packageName: String get() = CodexPetContract.packageName(manifest.id)
}

object CodexPetPackage {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    fun fromManifest(
        manifestFile: File,
        validateTransparency: Boolean = true,
    ): CodexPetSource {
        require(manifestFile.isFile) { "pet.json not found: ${manifestFile.absolutePath}" }
        val manifest = json.decodeFromString<CodexPetManifest>(manifestFile.readText())
        require(manifest.id.isNotBlank()) { "Codex pet id is empty" }
        require(manifest.displayName.isNotBlank()) { "Codex pet displayName is empty" }

        val packageRoot = requireNotNull(manifestFile.parentFile).canonicalFile
        val declaredAtlasFile = File(packageRoot, manifest.spritesheetPath).canonicalFile
        require(declaredAtlasFile.path.startsWith(packageRoot.path + File.separator)) {
            "spritesheetPath escapes the Codex pet package"
        }
        val atlasFile = resolveAtlasFile(packageRoot, declaredAtlasFile)
        return validatedSource(manifest, manifestFile, atlasFile, validateTransparency)
    }

    internal fun resolveAtlasFile(packageRoot: File, declaredAtlasFile: File): File {
        if (declaredAtlasFile.isFile) return declaredAtlasFile.canonicalFile

        val fallbackCandidates = packageRoot.listFiles().orEmpty().filter { file ->
            file.isFile && SUPPORTED_ATLAS_NAMES.any { supportedName ->
                file.name.equals(supportedName, ignoreCase = true)
            }
        }
        return fallbackCandidates.singleOrNull()?.canonicalFile ?: declaredAtlasFile
    }

    fun fromStandalone(atlasFile: File, displayName: String): CodexPetSource {
        val name = displayName.substringBeforeLast('.').trim().ifBlank { "Codex Pet" }
        val dimensions = decodeDimensions(atlasFile)
        val version = CodexPetContract.inferVersion(dimensions.first, dimensions.second)
        val manifest = CodexPetManifest(
            id = CodexPetContract.sanitizeId(name),
            displayName = name,
            description = "Imported Codex pet",
            spriteVersionNumber = version,
            spritesheetPath = atlasFile.name,
        )
        return validatedSource(manifest, null, atlasFile, validateTransparency = true)
    }

    fun findManifest(root: File): File? = root.walkTopDown()
        .maxDepth(4)
        .firstOrNull { it.isFile && it.name.equals("pet.json", ignoreCase = true) }

    fun install(
        source: CodexPetSource,
        skinsRoot: File,
        overwrite: Boolean,
        isBuiltin: Boolean,
        isActive: Boolean,
        author: String? = null,
        catalogVersion: String? = null,
    ): SkinEntity {
        check(skinsRoot.exists() || skinsRoot.mkdirs()) {
            "Could not create ${skinsRoot.absolutePath}"
        }
        val destination = File(skinsRoot, source.packageName)
        if (destination.exists() && !overwrite) {
            throw IllegalStateException("Skin already exists: ${source.packageName}")
        }
        if (destination.exists()) destination.deleteRecursively()
        check(destination.mkdirs()) { "Could not create ${destination.absolutePath}" }

        val atlasExtension = source.atlasFile.extension.lowercase().let {
            if (it == "png") "png" else "webp"
        }
        val installedAtlas = File(destination, "spritesheet.$atlasExtension")
        source.atlasFile.copyTo(installedAtlas, overwrite = true)

        val installedManifest = source.manifest.copy(
            id = CodexPetContract.sanitizeId(source.manifest.id),
            spriteVersionNumber = source.version,
            spritesheetPath = installedAtlas.name,
        )
        File(destination, "pet.json").writeText(json.encodeToString(installedManifest))

        val preview = File(destination, "preview.png")
        createPreview(installedAtlas, preview)

        return SkinEntity(
            packageName = source.packageName,
            name = installedManifest.displayName,
            author = author?.takeIf { it.isNotBlank() } ?: "Codex",
            previewPath = preview.name,
            isActive = isActive,
            isFavorite = false,
            isBuiltin = isBuiltin,
            version = catalogVersion?.takeIf { it.isNotBlank() } ?: "Codex v${source.version}",
        )
    }

    fun repairPreviewIfNeeded(manifestFile: File): Boolean {
        val destination = requireNotNull(manifestFile.parentFile)
        val preview = File(destination, "preview.png")
        if (preview.hasVisiblePixel()) return false

        val source = fromManifest(manifestFile, validateTransparency = false)
        createPreview(source.atlasFile, preview)
        return true
    }

    private fun validatedSource(
        manifest: CodexPetManifest,
        manifestFile: File?,
        atlasFile: File,
        validateTransparency: Boolean,
    ): CodexPetSource {
        require(atlasFile.isFile) { "Codex spritesheet not found: ${atlasFile.absolutePath}" }
        require(atlasFile.length() <= CodexPetContract.MAX_ATLAS_BYTES) {
            "Codex spritesheet exceeds 20 MiB"
        }
        val (width, height) = decodeDimensions(atlasFile)
        val inferredVersion = CodexPetContract.inferVersion(width, height)
        require(manifest.spriteVersionNumber == inferredVersion) {
            "pet.json declares spriteVersionNumber ${manifest.spriteVersionNumber}, " +
                "but the atlas dimensions require version $inferredVersion"
        }

        if (validateTransparency) {
            val bitmap = requireNotNull(BitmapFactory.decodeFile(atlasFile.absolutePath)) {
                "Could not decode Codex spritesheet"
            }
            try {
                require(bitmap.hasAlpha()) { "Codex spritesheet must have an alpha channel" }
                require(hasTransparentPixel(bitmap)) {
                    "Codex spritesheet must contain transparent background pixels"
                }
            } finally {
                bitmap.recycle()
            }
        }
        return CodexPetSource(manifest, manifestFile, atlasFile, inferredVersion)
    }

    private fun decodeDimensions(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        require(options.outWidth > 0 && options.outHeight > 0) {
            "Could not read Codex spritesheet dimensions"
        }
        return options.outWidth to options.outHeight
    }

    private fun hasTransparentPixel(bitmap: Bitmap): Boolean {
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            if (row.any { pixel -> (pixel ushr 24) != 0xff }) return true
        }
        return false
    }

    private fun createPreview(atlasFile: File, output: File) {
        val atlas = requireNotNull(BitmapFactory.decodeFile(atlasFile.absolutePath))
        try {
            val previewColumn = (0 until CodexPetContract.idle.frameCount)
                .maxByOrNull { column -> atlas.visiblePixelCount(row = 0, column = column) }
                ?: 0
            val neutral = Bitmap.createBitmap(
                atlas,
                previewColumn * CodexPetContract.CELL_WIDTH,
                0,
                CodexPetContract.CELL_WIDTH,
                CodexPetContract.CELL_HEIGHT,
            )
            neutral.useBitmap {
                output.outputStream().buffered().use { stream ->
                    check(it.compress(Bitmap.CompressFormat.PNG, 100, stream))
                }
            }
        } finally {
            atlas.recycle()
        }
    }

    private fun File.hasVisiblePixel(): Boolean {
        if (!isFile) return false
        val bitmap = BitmapFactory.decodeFile(absolutePath) ?: return false
        return bitmap.useBitmap { it.visiblePixelCount() > 0 }
    }

    private fun Bitmap.visiblePixelCount(row: Int = 0, column: Int = 0): Int {
        val startX = column * CodexPetContract.CELL_WIDTH
        val startY = row * CodexPetContract.CELL_HEIGHT
        if (
            startX + CodexPetContract.CELL_WIDTH > width ||
            startY + CodexPetContract.CELL_HEIGHT > height
        ) {
            return 0
        }

        val pixels = IntArray(CodexPetContract.CELL_WIDTH)
        var visible = 0
        repeat(CodexPetContract.CELL_HEIGHT) { y ->
            getPixels(
                pixels,
                0,
                CodexPetContract.CELL_WIDTH,
                startX,
                startY + y,
                CodexPetContract.CELL_WIDTH,
                1,
            )
            visible += pixels.count { pixel -> (pixel ushr 24) != 0 }
        }
        return visible
    }

    private inline fun <T> Bitmap.useBitmap(block: (Bitmap) -> T): T = try {
        block(this)
    } finally {
        recycle()
    }

    private val SUPPORTED_ATLAS_NAMES = setOf("spritesheet.png", "spritesheet.webp")
}
