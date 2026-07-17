package org.nqmgaming.aneko.core.pet

object CodexPetContract {
    const val COLUMNS = 8
    const val STANDARD_ROWS = 9
    const val V2_ROWS = 11
    const val CELL_WIDTH = 192
    const val CELL_HEIGHT = 208
    const val ATLAS_WIDTH = COLUMNS * CELL_WIDTH
    const val V1_ATLAS_HEIGHT = STANDARD_ROWS * CELL_HEIGHT
    const val V2_ATLAS_HEIGHT = V2_ROWS * CELL_HEIGHT
    const val MAX_ATLAS_BYTES = 20L * 1024L * 1024L

    data class Cell(val row: Int, val column: Int)

    data class AnimationRow(
        val row: Int,
        val durationsMs: IntArray,
    ) {
        val frameCount: Int get() = durationsMs.size
    }

    val idle = AnimationRow(0, intArrayOf(280, 110, 110, 140, 140, 320))
    val runningRight = AnimationRow(1, intArrayOf(120, 120, 120, 120, 120, 120, 120, 220))
    val runningLeft = AnimationRow(2, intArrayOf(120, 120, 120, 120, 120, 120, 120, 220))
    val waving = AnimationRow(3, intArrayOf(140, 140, 140, 280))
    val jumping = AnimationRow(4, intArrayOf(140, 140, 140, 140, 280))
    val failed = AnimationRow(5, intArrayOf(140, 140, 140, 140, 140, 140, 140, 240))
    val waiting = AnimationRow(6, intArrayOf(150, 150, 150, 150, 150, 260))
    val running = AnimationRow(7, intArrayOf(120, 120, 120, 120, 120, 220))
    val review = AnimationRow(8, intArrayOf(150, 150, 150, 150, 150, 280))

    fun inferVersion(width: Int, height: Int): Int = when {
        width == ATLAS_WIDTH && height == V1_ATLAS_HEIGHT -> 1
        width == ATLAS_WIDTH && height == V2_ATLAS_HEIGHT -> 2
        else -> throw IllegalArgumentException(
            "Codex pet atlas must be ${ATLAS_WIDTH}x$V1_ATLAS_HEIGHT or " +
                "${ATLAS_WIDTH}x$V2_ATLAS_HEIGHT; got ${width}x$height"
        )
    }

    fun expectedHeight(version: Int): Int = when (version) {
        1 -> V1_ATLAS_HEIGHT
        2 -> V2_ATLAS_HEIGHT
        else -> throw IllegalArgumentException("Unsupported Codex sprite version: $version")
    }

    fun lookCell(degrees: Int): Cell {
        val normalized = ((degrees % 360) + 360) % 360
        require(normalized in setOf(0, 45, 90, 135, 180, 225, 270, 315)) {
            "Only the eight ANeko movement directions are supported"
        }
        return if (normalized < 180) {
            Cell(row = 9, column = (normalized / 22.5).toInt().coerceIn(0, 7))
        } else {
            Cell(row = 10, column = ((normalized - 180) / 22.5).toInt().coerceIn(0, 7))
        }
    }

    fun sanitizeId(value: String): String {
        val normalized = value
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-', '.', '_')
        return normalized.ifBlank { "pet" }.take(80)
    }

    fun packageName(id: String): String = "codex.${sanitizeId(id)}"
}
