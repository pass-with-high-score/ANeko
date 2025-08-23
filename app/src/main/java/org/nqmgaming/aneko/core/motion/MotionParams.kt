package org.nqmgaming.aneko.core.motion

import android.util.AttributeSet
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.IOException

open class MotionParams {

    enum class MoveDirection { UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT }
    enum class WallDirection { UP, DOWN, LEFT, RIGHT }

    open var acceleration = 0f
    open var maxVelocity = 0f
    open var decelerationDistance = 0f
    open var proximityDistance = 0f

    open var initialState: String = "stop"
    open var awakeState: String = "awake"
    open var moveStatePrefix: String = "move"
    open var wallStatePrefix: String = "wall"

    // Chỉ đọc FILE: thư mục chứa ảnh + skin.xml
    internal var skinDir: File? = null
        private set

    data class Motion(
        var name: String = "",
        var nextState: String? = null,
        var checkMove: Boolean = false,
        var checkWall: Boolean = false,
        var items: MotionDrawable = MotionDrawable()
    )

    open val motions = HashMap<String, Motion>()

    fun hasState(state: String) = motions.containsKey(state)
    fun getNextState(state: String) = motions[state]?.nextState
    fun needCheckMove(state: String) = motions[state]?.checkMove == true
    fun needCheckWall(state: String) = motions[state]?.checkWall == true
    fun getDrawable(state: String) = motions[state]?.items

    fun getMoveState(dir: MoveDirection) = when (dir) {
        MoveDirection.UP -> "${moveStatePrefix}Up"
        MoveDirection.DOWN -> "${moveStatePrefix}Down"
        MoveDirection.LEFT -> "${moveStatePrefix}Left"
        MoveDirection.RIGHT -> "${moveStatePrefix}Right"
        MoveDirection.UP_LEFT -> "${moveStatePrefix}UpLeft"
        MoveDirection.UP_RIGHT -> "${moveStatePrefix}UpRight"
        MoveDirection.DOWN_LEFT -> "${moveStatePrefix}DownLeft"
        MoveDirection.DOWN_RIGHT -> "${moveStatePrefix}DownRight"
    }

    fun getWallState(dir: WallDirection) = when (dir) {
        WallDirection.UP -> "${wallStatePrefix}Up"
        WallDirection.DOWN -> "${wallStatePrefix}Down"
        WallDirection.LEFT -> "${wallStatePrefix}Left"
        WallDirection.RIGHT -> "${wallStatePrefix}Right"
    }

    // ---------- Parsing from FILE (no Resources) ----------
    companion object {
        private const val TAG_MOTION_PARAMS = "motion-params"
        private const val TAG_MOTION = "motion"
        private const val TAG_ITEM = "item"
        private const val TAG_REPEAT_ITEM = "repeat-item"

        private const val ATTR_ACCELERATION = "acceleration"
        private const val ATTR_MAX_VELOCITY = "maxVelocity"
        private const val ATTR_DECELERATION = "decelerationDistance"
        private const val ATTR_DEACCELERATION =
            "deaccelerationDistance"
        private const val ATTR_PROXIMITY = "proximityDistance"

        private const val ATTR_INITIAL_STATE = "initialState"
        private const val ATTR_AWAKE_STATE = "awakeState"
        private const val ATTR_MOVE_STATE_PREFIX = "moveStatePrefix"
        private const val ATTR_WALL_STATE_PREFIX = "wallStatePrefix"

        private const val ATTR_STATE = "state"
        private const val ATTR_DURATION = "duration"
        private const val ATTR_NEXT_STATE = "nextState"
        private const val ATTR_CHECK_WALL = "checkWall"
        private const val ATTR_CHECK_MOVE = "checkMove"

        private const val ATTR_ITEM_DRAWABLE = "drawable"
        private const val ATTR_ITEM_DURATION = "duration"
        private const val ATTR_ITEM_REPEAT_COUNT = "repeatCount"

        private const val DEF_ACCELERATION = 160
        private const val DEF_MAX_VELOCITY = 100
        private const val DEF_DECELERATE_DISTANCE = 100
        private const val DEF_PROXIMITY_DISTANCE = 10

        @Throws(Exception::class)
        fun parseFromFile(
            skinXml: File,
            skinDir: File,
            displayMetrics: android.util.DisplayMetrics
        ): MotionParams {
            if (!skinXml.exists()) {
                throw IllegalArgumentException("skin.xml not found: $skinXml")
            }
            FileInputStream(skinXml).use { fis ->
                val parser = Xml.newPullParser()
                parser.setInput(fis, null)
                val attrs = Xml.asAttributeSet(parser)
                val params = MotionParams()
                params.skinDir = skinDir
                params.parseXml(displayMetrics, parser, attrs)
                return params
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseXml(dm: android.util.DisplayMetrics, xml: XmlPullParser, attrs: AttributeSet) {
        val startDepth = xml.depth
        while (true) {
            val type = xml.next()
            if (type == XmlPullParser.END_DOCUMENT ||
                (type == XmlPullParser.END_TAG && startDepth >= xml.depth)
            ) break
            if (type != XmlPullParser.START_TAG) continue

            when (xml.name) {
                TAG_MOTION_PARAMS -> parseMotionParams(dm, xml, attrs)
                else -> throw IllegalArgumentException("unknown tag: ${xml.name}")
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseMotionParams(
        dm: android.util.DisplayMetrics,
        xml: XmlPullParser,
        attrs: AttributeSet
    ) {
        val density = dm.density

        fun attrInt(name: String, def: Int) = attrs.getAttributeIntValue(null, name, def)
        fun attrIntEither(primary: String, fallback: String, def: Int): Int {
            val v = attrs.getAttributeIntValue(null, primary, Int.MIN_VALUE)
            return if (v == Int.MIN_VALUE) attrs.getAttributeIntValue(null, fallback, def) else v
        }

        fun attrStr(name: String, def: String) = attrs.getAttributeValue(null, name) ?: def
        fun attrBool(name: String, def: Boolean) = attrs.getAttributeBooleanValue(null, name, def)

        acceleration = density * attrInt(ATTR_ACCELERATION, DEF_ACCELERATION)
        decelerationDistance =
            density * attrIntEither(ATTR_DECELERATION, ATTR_DEACCELERATION, DEF_DECELERATE_DISTANCE)
        maxVelocity = density * attrInt(ATTR_MAX_VELOCITY, DEF_MAX_VELOCITY)
        proximityDistance = density * attrInt(ATTR_PROXIMITY, DEF_PROXIMITY_DISTANCE)

        initialState = attrStr(ATTR_INITIAL_STATE, "stop")
        awakeState = attrStr(ATTR_AWAKE_STATE, "awake")
        moveStatePrefix = attrStr(ATTR_MOVE_STATE_PREFIX, "move")
        wallStatePrefix = attrStr(ATTR_WALL_STATE_PREFIX, "wall")

        val startDepth = xml.depth
        while (true) {
            val type = xml.next()
            if (type == XmlPullParser.END_DOCUMENT ||
                (type == XmlPullParser.END_TAG && startDepth >= xml.depth)
            ) break
            if (type != XmlPullParser.START_TAG) continue

            when (xml.name) {
                TAG_MOTION -> parseMotion(xml, attrs, ::attrBool)
                else -> throw IllegalArgumentException("unknown tag: ${xml.name}")
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseMotion(
        xml: XmlPullParser,
        attrs: AttributeSet,
        boolAttr: (String, Boolean) -> Boolean,
    ) {
        val motion = Motion()

        motion.name = attrs.getAttributeValue(null, ATTR_STATE)
            ?: throw IllegalArgumentException("state is not specified: ${attrs.positionDescription}")

        val duration = attrs.getAttributeIntValue(null, ATTR_DURATION, -1)
        motion.nextState = attrs.getAttributeValue(null, ATTR_NEXT_STATE)
        motion.checkMove = boolAttr(ATTR_CHECK_MOVE, false)
        motion.checkWall = boolAttr(ATTR_CHECK_WALL, false)

        motion.items = MotionDrawable()

        val startDepth = xml.depth
        while (true) {
            val type = xml.next()
            if (type == XmlPullParser.END_DOCUMENT ||
                (type == XmlPullParser.END_TAG && startDepth >= xml.depth)
            ) break
            if (type != XmlPullParser.START_TAG) continue

            when (xml.name) {
                TAG_ITEM -> parseItem(motion.items, attrs)
                TAG_REPEAT_ITEM -> parseRepeatItem(motion.items, xml, attrs)
                else -> throw IllegalArgumentException("unknown tag: ${xml.name}")
            }
        }

        motion.items.setTotalDuration(duration)
        motion.items.setRepeatCount(1)
        motions[motion.name] = motion
    }

    private fun parseItem(items: MotionDrawable, attrs: AttributeSet) {
        val name = (attrs.getAttributeValue(null, ATTR_ITEM_DRAWABLE) ?: "").trim()
        val duration = attrs.getAttributeIntValue(null, ATTR_ITEM_DURATION, -1)
        val dir = skinDir ?: throw IllegalStateException("skinDir is null")
        items.addFrameFromFile(dir, name, duration)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseRepeatItem(items: MotionDrawable, xml: XmlPullParser, attrs: AttributeSet) {
        val duration = attrs.getAttributeIntValue(null, ATTR_ITEM_DURATION, -1)
        val repeat = attrs.getAttributeIntValue(null, ATTR_ITEM_REPEAT_COUNT, -1)
        val dr = MotionDrawable()

        val startDepth = xml.depth
        while (true) {
            val type = xml.next()
            if (type == XmlPullParser.END_DOCUMENT ||
                (type == XmlPullParser.END_TAG && startDepth >= xml.depth)
            ) break
            if (type != XmlPullParser.START_TAG) continue

            when (xml.name) {
                TAG_ITEM -> parseItem(dr, attrs)
                TAG_REPEAT_ITEM -> parseRepeatItem(dr, xml, attrs)
                else -> throw IllegalArgumentException("unknown tag: ${xml.name}")
            }
        }

        dr.setTotalDuration(duration)
        dr.setRepeatCount(repeat)
        items.addFrame(dr, -1)
    }
}
