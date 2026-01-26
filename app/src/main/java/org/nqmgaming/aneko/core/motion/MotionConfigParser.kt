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

    fun parseItem(items: MotionDrawable, attrs: AttributeSet) {
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
    private fun parseRepeatItem(
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

            when (val name = xml.name) {
                TAG_ITEM -> parseItem(dr, attrs)
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

        private const val TAG_ITEM = "item"
        private const val TAG_REPEAT_ITEM = "repeat-item"

        private const val ATTR_ITEM_DRAWABLE = "drawable"
        private const val ATTR_ITEM_DURATION = "duration"
        private const val ATTR_ITEM_REPEAT_COUNT = "repeatCount"
    }
}
