package org.nqmgaming.aneko.core.motion

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.nqmgaming.aneko.core.pet.CodexPetContract
import org.nqmgaming.aneko.core.pet.CodexPetPackage
import java.io.File

class CodexPetMotionParams(
    resources: Resources,
    manifestFile: File,
) : MotionParams() {

    override val motions = HashMap<String, Motion>()
    private val atlas: Atlas

    init {
        val source = CodexPetPackage.fromManifest(
            manifestFile = manifestFile,
            validateTransparency = false,
        )
        atlas = Atlas(resources, source.atlasFile, source.version)

        val density = resources.displayMetrics.density
        acceleration = 160 * density
        decelerationDistance = 60 * density
        maxVelocity = 100 * density
        proximityDistance = 10 * density
        initialState = "stop"
        awakeState = "awake"
        moveStatePrefix = "move"
        wallStatePrefix = "wall"

        addRowMotion(
            name = "stop",
            row = CodexPetContract.idle,
            repeatCount = 3,
            nextState = "wait",
            checkWall = true,
        )
        addRowMotion(
            name = "wait",
            row = CodexPetContract.waiting,
            repeatCount = 1,
            nextState = "stop",
        )
        addRowMotion(
            name = "awake",
            row = CodexPetContract.jumping,
            repeatCount = 1,
            nextState = "stop",
            checkMove = true,
        )

        addRowMotion("moveRight", CodexPetContract.runningRight, repeatCount = -1)
        addRowMotion("moveLeft", CodexPetContract.runningLeft, repeatCount = -1)

        if (source.version == 2) {
            addLookMotion("moveUp", 0, repeatCount = -1)
            addLookMotion("moveUpRight", 45, repeatCount = -1)
            addLookMotion("moveDownRight", 135, repeatCount = -1)
            addLookMotion("moveDown", 180, repeatCount = -1)
            addLookMotion("moveDownLeft", 225, repeatCount = -1)
            addLookMotion("moveUpLeft", 315, repeatCount = -1)

            addLookMotion("wallUp", 0, repeatCount = 1, nextState = "wait")
            addLookMotion("wallRight", 90, repeatCount = 1, nextState = "wait")
            addLookMotion("wallDown", 180, repeatCount = 1, nextState = "wait")
            addLookMotion("wallLeft", 270, repeatCount = 1, nextState = "wait")
        } else {
            addRowMotion("moveUp", CodexPetContract.running, repeatCount = -1)
            addRowMotion("moveDown", CodexPetContract.running, repeatCount = -1)
            addRowMotion("moveUpRight", CodexPetContract.runningRight, repeatCount = -1)
            addRowMotion("moveDownRight", CodexPetContract.runningRight, repeatCount = -1)
            addRowMotion("moveUpLeft", CodexPetContract.runningLeft, repeatCount = -1)
            addRowMotion("moveDownLeft", CodexPetContract.runningLeft, repeatCount = -1)
            addRowMotion("wallUp", CodexPetContract.waving, repeatCount = 1, nextState = "wait")
            addRowMotion("wallRight", CodexPetContract.waving, repeatCount = 1, nextState = "wait")
            addRowMotion("wallDown", CodexPetContract.waving, repeatCount = 1, nextState = "wait")
            addRowMotion("wallLeft", CodexPetContract.waving, repeatCount = 1, nextState = "wait")
        }

        addRowMotion("waving", CodexPetContract.waving, repeatCount = -1)
        addRowMotion("jumping", CodexPetContract.jumping, repeatCount = -1)
        addRowMotion("failed", CodexPetContract.failed, repeatCount = -1)
        addRowMotion("waiting", CodexPetContract.waiting, repeatCount = -1)
        addRowMotion("running", CodexPetContract.running, repeatCount = -1)
        addRowMotion("review", CodexPetContract.review, repeatCount = -1)
    }

    private fun addRowMotion(
        name: String,
        row: CodexPetContract.AnimationRow,
        repeatCount: Int,
        nextState: String? = null,
        checkMove: Boolean = false,
        checkWall: Boolean = false,
    ) {
        val drawable = MotionDrawable()
        row.durationsMs.forEachIndexed { column, duration ->
            drawable.addFrame(atlas.drawable(row.row, column), duration)
        }
        drawable.setRepeatCount(repeatCount)
        if (repeatCount != 1) drawable.setTotalDuration(-1)
        motions[name] = Motion(
            name = name,
            nextState = nextState,
            checkMove = checkMove,
            checkWall = checkWall,
            items = drawable,
        )
    }

    private fun addLookMotion(
        name: String,
        degrees: Int,
        repeatCount: Int,
        nextState: String? = null,
    ) {
        val cell = CodexPetContract.lookCell(degrees)
        val drawable = MotionDrawable().apply {
            addFrame(atlas.drawable(cell.row, cell.column), 750)
            setRepeatCount(repeatCount)
            if (repeatCount != 1) setTotalDuration(-1)
        }
        motions[name] = Motion(name = name, nextState = nextState, items = drawable)
    }

    private class Atlas(
        private val resources: Resources,
        file: File,
        version: Int,
    ) {
        private val bitmap: Bitmap = requireNotNull(BitmapFactory.decodeFile(file.absolutePath))
        private val cells = HashMap<Pair<Int, Int>, Bitmap>()

        init {
            require(bitmap.width == CodexPetContract.ATLAS_WIDTH)
            require(bitmap.height == CodexPetContract.expectedHeight(version))
        }

        fun drawable(row: Int, column: Int): Drawable {
            require(row in 0 until bitmap.height / CodexPetContract.CELL_HEIGHT)
            require(column in 0 until CodexPetContract.COLUMNS)
            val cell = cells.getOrPut(row to column) {
                Bitmap.createBitmap(
                    bitmap,
                    column * CodexPetContract.CELL_WIDTH,
                    row * CodexPetContract.CELL_HEIGHT,
                    CodexPetContract.CELL_WIDTH,
                    CodexPetContract.CELL_HEIGHT,
                )
            }
            return BitmapDrawable(resources, cell)
        }
    }
}
