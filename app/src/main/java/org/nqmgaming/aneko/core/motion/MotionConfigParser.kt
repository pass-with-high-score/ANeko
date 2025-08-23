package org.nqmgaming.aneko.core.motion

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Locale

class MotionConfigParser(
    res: Resources,
    baseDir: File,
    xmlFile: String
) : MotionParams() {

    private val resourceBaseDir: String = baseDir.absolutePath
    override var motions = HashMap<String, Motion>()

    init {
        try {
            val skinXml = File(baseDir, xmlFile)
            val params = parseFromFile(skinXml, baseDir, res.displayMetrics)

            this.acceleration = params.acceleration
            this.decelerationDistance = params.decelerationDistance
            this.maxVelocity = params.maxVelocity
            this.proximityDistance = params.proximityDistance
            this.initialState = params.initialState
            this.awakeState = params.awakeState
            this.moveStatePrefix = params.moveStatePrefix
            this.wallStatePrefix = params.wallStatePrefix
            this.motions.putAll(params.motions)

        } catch (e: Exception) {
            Timber.e(e)
            throw IllegalArgumentException(e)
        }
    }
    // ====== Parsing ======

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseXml(res: Resources, xml: XmlPullParser, attrs: AttributeSet) {
        val depth = xml.depth
        while (true) {
            val type = xml.next()
            if (type == XmlPullParser.END_DOCUMENT ||
                (type == XmlPullParser.END_TAG && depth >= xml.depth)
            ) break
            if (type != XmlPullParser.START_TAG) continue

            val name = xml.name
            if (TAG_MOTION_PARAMS == name) {
                parseMotionParams(res, xml, attrs)
            } else {
                throw IllegalArgumentException("unknown tag: $name")
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseMotionParams(res: Resources, xml: XmlPullParser, attrs: AttributeSet) {
        val density = res.displayMetrics.density

        fun ai(name: String, def: Int) = attrs.getAttributeIntValue(null, name, def)
        fun aiEither(primary: String, fallback: String, def: Int): Int {
            val v = attrs.getAttributeIntValue(null, primary, Int.MIN_VALUE)
            return if (v == Int.MIN_VALUE) attrs.getAttributeIntValue(null, fallback, def) else v
        }

        fun asStr(name: String, def: String) = attrs.getAttributeValue(null, name) ?: def

        // Ghi đúng field public của MotionParams (bản Java)
        acceleration = density * ai(ATTR_ACCELERATION, DEF_ACCELERATION)
        // Bản cũ có nhầm ATTR_ACCELERATION: sửa lại đọc deceleration và hỗ trợ alias "deaccelerationDistance"
        decelerationDistance =
            density * aiEither(ATTR_DECELERATION, ATTR_DEACCELERATION, DEF_DECELERATE_DISTANCE)
        maxVelocity = density * ai(ATTR_MAX_VELOCITY, DEF_MAX_VELOCITY)
        proximityDistance = density * ai(ATTR_PROXIMITY, DEF_PROXIMITY_DISTANCE)

        initialState = asStr(ATTR_INITIAL_STATE, DEF_INITIAL_STATE)
        awakeState = asStr(ATTR_AWAKE_STATE, DEF_AWAKE_STATE)
        moveStatePrefix = asStr(ATTR_MOVE_STATE_PREFIX, DEF_MOVE_STATE_PREFIX)
        wallStatePrefix = asStr(ATTR_WALL_STATE_PREFIX, DEF_WALL_STATE_PREFIX)

        val depth = xml.depth
        while (true) {
            val type = xml.next()
            if (type == XmlPullParser.END_DOCUMENT ||
                (type == XmlPullParser.END_TAG && depth >= xml.depth)
            ) break
            if (type != XmlPullParser.START_TAG) continue

            val name = xml.name
            if (TAG_MOTION == name) {
                parseMotion(res, xml, attrs)
            } else {
                throw IllegalArgumentException("unknown tag: $name")
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseMotion(res: Resources, xml: XmlPullParser, attrs: AttributeSet) {
        val motion = Motion()

        motion.name = attrs.getAttributeValue(null, ATTR_STATE)
            ?: throw IllegalArgumentException("state is not specified: ${attrs.positionDescription}")

        val duration = attrs.getAttributeIntValue(null, ATTR_DURATION, -1)
        motion.nextState = attrs.getAttributeValue(null, ATTR_NEXT_STATE)
        motion.checkMove = attrs.getAttributeBooleanValue(null, ATTR_CHECK_MOVE, false)
        motion.checkWall = attrs.getAttributeBooleanValue(null, ATTR_CHECK_WALL, false)

        motion.items = MotionDrawable()

        val depth = xml.depth
        while (true) {
            val type = xml.next()
            if (type == XmlPullParser.END_DOCUMENT ||
                (type == XmlPullParser.END_TAG && depth >= xml.depth)
            ) break
            if (type != XmlPullParser.START_TAG) continue

            val name = xml.name
            when (name) {
                TAG_ITEM -> parseItem(res, motion.items, xml, attrs)
                TAG_REPEAT_ITEM -> parseRepeatItem(res, motion.items, xml, attrs)
                else -> throw IllegalArgumentException("unknown tag: $name")
            }
        }

        motion.items.setTotalDuration(duration)
        motion.items.setRepeatCount(1)

        this.motions[motion.name] = motion
    }

    fun parseItem(res: Resources, items: MotionDrawable, xml: XmlPullParser, attrs: AttributeSet) {
        val duration = attrs.getAttributeIntValue(null, ATTR_ITEM_DURATION, -1)
        val filename = (attrs.getAttributeValue(null, ATTR_ITEM_DRAWABLE) ?: "").trim()

        val start = attrs.getAttributeIntValue(null, "start", 0)
        val end = attrs.getAttributeIntValue(null, "end", -1)

        if (end > start) {
            // Tạo chuỗi dạng prefix%03d.png theo quy tắc cũ
            val prefix = filename.replaceFirst("\\d+$".toRegex(), "")
            val fmt = "$resourceBaseDir/$prefix%03d.png"
            for (i in start..end) {
                val path = String.format(Locale.KOREAN, fmt, i)
                val d = Drawable.createFromPath(path)
                if (d != null) items.addFrame(d, duration)
            }
        } else {
            val d = Drawable.createFromPath("$resourceBaseDir/$filename.png")
            requireNotNull(d) { "Frame image not found: $resourceBaseDir/$filename.png" }
            items.addFrame(d, duration)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseRepeatItem(
        res: Resources,
        items: MotionDrawable,
        xml: XmlPullParser,
        attrs: AttributeSet
    ) {
        val duration = attrs.getAttributeIntValue(null, ATTR_ITEM_DURATION, -1)
        val repeat = attrs.getAttributeIntValue(null, ATTR_ITEM_REPEAT_COUNT, -1)
        val dr = MotionDrawable()

        val depth = xml.depth
        while (true) {
            val type = xml.next()
            if (type == XmlPullParser.END_DOCUMENT ||
                (type == XmlPullParser.END_TAG && depth >= xml.depth)
            ) break
            if (type != XmlPullParser.START_TAG) continue

            val name = xml.name
            when (name) {
                TAG_ITEM -> parseItem(res, dr, xml, attrs)
                TAG_REPEAT_ITEM -> parseRepeatItem(res, dr, xml, attrs)
                else -> throw IllegalArgumentException("unknown tag: $name")
            }
        }

        dr.setTotalDuration(duration)
        dr.setRepeatCount(repeat)
        items.addFrame(dr, -1)
    }

    companion object {
        @JvmStatic
        @Throws(Exception::class)
        fun parseFromFile(context: Context, skinXml: File, skinDir: File): MotionParams {
            return MotionConfigParser(context.resources, skinDir, skinXml.name)
        }

        private const val TAG_MOTION_PARAMS = "motion-params"
        private const val TAG_MOTION = "motion"
        private const val TAG_ITEM = "item"
        private const val TAG_REPEAT_ITEM = "repeat-item"

        private const val ATTR_ACCELERATION = "acceleration"
        private const val ATTR_MAX_VELOCITY = "maxVelocity"
        private const val ATTR_DECELERATION = "decelerationDistance"
        private const val ATTR_DEACCELERATION = "deaccelerationDistance" // alias
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

        private const val DEF_INITIAL_STATE = "stop"
        private const val DEF_AWAKE_STATE = "awake"
        private const val DEF_MOVE_STATE_PREFIX = "move"
        private const val DEF_WALL_STATE_PREFIX = "wall"
    }
}
