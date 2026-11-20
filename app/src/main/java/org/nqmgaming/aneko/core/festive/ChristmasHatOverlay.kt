package org.nqmgaming.aneko.core.festive

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

object ChristmasHatOverlay {

    private val paint by lazy {
        Paint().apply {
            isFilterBitmap = false
            isDither = false
        }
    }

    // Palette colors (ARGB)
    private const val B = 0xFF000000.toInt() // Black outline
    private const val R = 0xFFE53935.toInt() // Christmas Red
    private const val r = 0xFFC62828.toInt() // Darker red (shade)
    private const val W = 0xFFFFFFFF.toInt() // White fur trim / pompom
    private const val w = 0xFFE0E0E0.toInt() // White fur shadow

    enum class HatPose {
        FRONT,
        RIGHT,
        LEFT,
        UP,
        DOWN,
        UPRIGHT,
        UPLEFT,
        SLEEP,
        KAKI
    }

    val bitmaps = HashMap<HatPose, Bitmap>()
    private var initialized = false

    @Synchronized
    fun ensureBitmaps() {
        if (initialized) return
        createBitmaps()
        initialized = true
    }

    private fun createBitmaps() {
        val artFront = listOf(
            "    BB    ",
            "   BRRB WW",
            "  BRRRRBWW",
            "  BRRRRRBB",
            " BRRRRRRB ",
            " BWWWWWWB ",
            "  BwwwwB  "
        )

        val artRight = listOf(
            " WW       ",
            "WWBBBB    ",
            " BBBrrB   ",
            "  BRRRrB  ",
            "  BRRRRRB ",
            "  BWWWWWB ",
            "   BwwwB  "
        )

        val artUp = listOf(
            "    BB    ",
            "   BRrB WW",
            "  BRRRrBWW",
            "  BRRRRrBB",
            " BRRRRRrB ",
            " BWWWWWWB ",
            "  BwwwwB  "
        )

        val artSleep = listOf(
            "   BB     ",
            "  BRRB    ",
            " BRRRRB WW",
            " BRRRRRBWW",
            "BWWWWWWBBB",
            " BwwwwB   "
        )

        val artKaki = listOf(
            "   BB     ",
            "  BRRB    ",
            " BRRRRB WW",
            " BRRRRRBWW",
            " BRRRRRRB ",
            " BWWWWWWB ",
            "  BwwwwB  "
        )

        bitmaps[HatPose.FRONT] = renderHatBitmap(artFront, startX = 12, startY = 1)
        bitmaps[HatPose.RIGHT] = renderHatBitmap(artRight, startX = 6, startY = 0)
        bitmaps[HatPose.LEFT] = renderHatBitmap(artRight, startX = 6, startY = 0, flipHorizontal = true)
        bitmaps[HatPose.UP] = renderHatBitmap(artUp, startX = 12, startY = 0)
        bitmaps[HatPose.DOWN] = renderHatBitmap(artFront, startX = 12, startY = 10)
        bitmaps[HatPose.UPRIGHT] = renderHatBitmap(artRight, startX = 12, startY = 0)
        bitmaps[HatPose.UPLEFT] = renderHatBitmap(artRight, startX = 12, startY = 0, flipHorizontal = true)
        bitmaps[HatPose.SLEEP] = renderHatBitmap(artSleep, startX = 11, startY = 11)
        bitmaps[HatPose.KAKI] = renderHatBitmap(artKaki, startX = 8, startY = 2)
    }

    private fun renderHatBitmap(
        art: List<String>,
        startX: Int,
        startY: Int,
        flipHorizontal: Boolean = false
    ): Bitmap {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for ((rowIdx, line) in art.withIndex()) {
            val y32 = startY + rowIdx
            for ((colIdx, ch) in line.withIndex()) {
                val color = when (ch) {
                    'B' -> B
                    'R' -> R
                    'r' -> r
                    'W' -> W
                    'w' -> w
                    else -> 0
                }
                if (color != 0) {
                    val rawX32 = startX + colIdx
                    val x32 = if (flipHorizontal) 31 - rawX32 else rawX32
                    val px = x32 * 2
                    val py = y32 * 2
                    if (px in 0..62 && py in 0..62) {
                        bmp.setPixel(px, py, color)
                        bmp.setPixel(px + 1, py, color)
                        bmp.setPixel(px, py + 1, color)
                        bmp.setPixel(px + 1, py + 1, color)
                    }
                }
            }
        }
        return bmp
    }

    fun getHatPose(frameName: String?): HatPose {
        if (frameName == null) return HatPose.FRONT
        val lower = frameName.lowercase()
        return when {
            lower.startsWith("mati") || lower.startsWith("awake") ||
                lower.startsWith("akubi") || lower.startsWith("icon") -> HatPose.FRONT

            lower.startsWith("kaki") -> HatPose.KAKI

            lower.startsWith("sleep") -> HatPose.SLEEP

            lower.startsWith("upright") -> HatPose.UPRIGHT

            lower.startsWith("upleft") -> HatPose.UPLEFT

            lower.startsWith("right") || lower.startsWith("dwright") || lower.startsWith("rtogi") -> HatPose.RIGHT

            lower.startsWith("left") || lower.startsWith("dwleft") || lower.startsWith("ltogi") -> HatPose.LEFT

            lower.startsWith("up") || lower.startsWith("utogi") -> HatPose.UP

            lower.startsWith("down") || lower.startsWith("dtogi") -> HatPose.DOWN

            else -> HatPose.FRONT
        }
    }

    fun drawHat(canvas: Canvas, bounds: Rect, frameName: String?, alpha: Int = 0xFF) {
        ensureBitmaps()
        val pose = getHatPose(frameName)
        val bitmap = bitmaps[pose] ?: return
        paint.alpha = alpha
        canvas.drawBitmap(bitmap, null, bounds, paint)
    }
}
