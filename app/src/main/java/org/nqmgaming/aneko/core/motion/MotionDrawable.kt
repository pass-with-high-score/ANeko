package org.nqmgaming.aneko.core.motion

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.core.graphics.drawable.toDrawable
import java.io.File

class MotionDrawable() : Drawable(), Animatable {

    interface OnMotionEndListener {
        fun onMotionEnd(drawable: MotionDrawable)
    }

    // ----- State -----
    private val constantState = MotionConstantState()

    private var curFrame = -1
    private var curRepeat = 0
    private var curDuration = -1
    private var onEnd: OnMotionEndListener? = null

    private var alphaValue: Int = 0xFF
    private var colorFilterValue: ColorFilter? = null

    private val frameUpdater = Runnable { updateFrame() }
    private val childCallback = ChildCallback()
    private val childEnd = ChildOnMotionEnd()

    constructor(anim: AnimationDrawable) : this() {
        constantState.repeatCount = if (anim.isOneShot) 1 else -1
        val nf = anim.numberOfFrames
        for (i in 0 until nf) {
            addFrame(anim.getFrame(i), anim.getDuration(i))
        }
    }

    // ----- API call from parser / logic move -----
    fun setTotalDuration(duration: Int) {
        constantState.totalDuration = duration
    }

    fun setRepeatCount(count: Int) {
        constantState.repeatCount = count
    }

    fun addFrame(drawable: Drawable, duration: Int) {
        var d = drawable
        if (d is AnimationDrawable) {
            val md = MotionDrawable(d)
            md.setTotalDuration(duration)
            d = md
        }
        if (d is MotionDrawable) {
            d.setOnMotionEndListener(childEnd)
        }
        d.callback = childCallback
        constantState.addFrame(d, duration)
    }

    private fun currentFrame(): Drawable? = constantState.getFrame(curFrame)

    fun setOnMotionEndListener(listener: OnMotionEndListener?) {
        onEnd = listener
    }

    private fun invokeOnMotionEndListener() {
        onEnd?.onMotionEnd(this)
    }

    // ----- Drawable overrides -----
    override fun getIntrinsicWidth(): Int = currentFrame()?.intrinsicWidth ?: -1

    override fun getIntrinsicHeight(): Int = currentFrame()?.intrinsicHeight ?: -1

    override fun getConstantState(): ConstantState = constantState

    override fun draw(canvas: Canvas) {
        currentFrame()?.draw(canvas)
    }

    @Deprecated("Deprecated in Java but still required by Drawable contract")
    override fun getOpacity(): Int {
        val c = currentFrame()
        return if (c == null || !c.isVisible) PixelFormat.TRANSPARENT else constantState.opacity
    }

    override fun setAlpha(alpha: Int) {
        if (alphaValue != alpha) {
            alphaValue = alpha
            currentFrame()?.alpha = alphaValue
        }
    }

    @Suppress("DEPRECATION")
    override fun setColorFilter(colorFilter: ColorFilter?) {
        if (colorFilterValue !== colorFilter) {
            colorFilterValue = colorFilter
            currentFrame()?.colorFilter = colorFilterValue
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        currentFrame()?.bounds = bounds
    }

    override fun onLevelChange(level: Int): Boolean {
        return currentFrame()?.setLevel(level) ?: false
    }

    override fun onStateChange(stateSet: IntArray): Boolean {
        return currentFrame()?.setState(stateSet) ?: false
    }

    override fun isStateful(): Boolean {
        return currentFrame()?.isStateful == true
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        currentFrame()?.setVisible(visible, restart)
        if (visible) {
            if (changed || restart) {
                stop()
                start()
            }
        } else {
            stop()
        }
        return changed
    }

    // ----- Animatable -----
    override fun isRunning(): Boolean = curDuration >= 0

    override fun start() {
        if (!isRunning) {
            curFrame = -1
            curRepeat = 0
            curDuration = 0
            updateFrame()
        }
    }

    override fun stop() {
        if (isRunning) {
            unscheduleSelf(frameUpdater)
            curDuration = -1
        }
    }

    // ----- Change frame -----
    private fun updateFrame() {
        val nf = constantState.frameCount
        var next = curFrame + 1
        var nextRepeat = curRepeat
        if (next >= nf) {
            next = 0
            nextRepeat = curRepeat + 1
            if (constantState.repeatCount >= 0 && nextRepeat >= constantState.repeatCount) {
                curDuration = -1
                invokeOnMotionEndListener()
                return
            }
        }

        if (constantState.totalDuration >= 0 && curDuration >= constantState.totalDuration) {
            curDuration = -1
            invokeOnMotionEndListener()
            return
        }

        currentFrame()?.setVisible(false, false)

        curFrame = next
        curRepeat = nextRepeat

        val nextDrawable = constantState.getFrame(next)!!
        nextDrawable.setVisible(isVisible, true)
        nextDrawable.alpha = alphaValue
        @Suppress("DEPRECATION")
        nextDrawable.colorFilter = colorFilterValue
        nextDrawable.state = state
        nextDrawable.level = level
        nextDrawable.bounds = bounds

        var duration = constantState.getFrameDuration(next)
        val nextDuration =
            if (duration < 0 && constantState.totalDuration < 0) {
                -1
            } else if (duration < 0) {
                constantState.totalDuration - curDuration
            } else if (constantState.totalDuration < 0) {
                curDuration + duration
            } else {
                minOf(curDuration + duration, constantState.totalDuration)
            }

        if (nextDuration >= 0) {
            duration = nextDuration - curDuration
            scheduleSelf(frameUpdater, SystemClock.uptimeMillis() + duration)
        }
        curDuration = if (nextDuration >= 0) nextDuration else curDuration
        invalidateSelf()
    }

    // ----- ConstantState -----
    private class ItemInfo(val drawable: Drawable, val duration: Int)

    private class MotionConstantState : ConstantState() {
        private val drawables = ArrayList<ItemInfo>()
        var changingConfigurationsMask: Int = 0
            private set
        var opacity: Int = PixelFormat.TRANSPARENT
            private set

        var totalDuration: Int = 0
        var repeatCount: Int = 1

        fun addFrame(drawable: Drawable, duration: Int) {
            drawables.add(ItemInfo(drawable, duration))
            totalDuration = if (duration >= 0 && totalDuration >= 0) {
                totalDuration + duration
            } else {
                -1
            }
            changingConfigurationsMask =
                changingConfigurationsMask or drawable.changingConfigurations
            opacity = if (drawables.size > 1) {
                resolveOpacity(opacity, drawables.last().drawable.opacity)
            } else {
                drawables.last().drawable.opacity
            }
        }

        fun getFrame(idx: Int): Drawable? {
            val i = idx.coerceAtLeast(0)
            return if (i >= drawables.size) null else drawables[i].drawable
        }

        fun getFrameDuration(idx: Int): Int {
            val i = idx.coerceAtLeast(0)
            return if (i >= drawables.size) 0 else drawables[i].duration
        }

        val frameCount: Int get() = drawables.size

        override fun getChangingConfigurations(): Int = changingConfigurationsMask

        override fun newDrawable(): Drawable {
            throw UnsupportedOperationException("newDrawable is not supported")
        }

        override fun newDrawable(res: Resources?): Drawable {
            throw UnsupportedOperationException("newDrawable(Resources) is not supported")
        }

        override fun newDrawable(res: Resources?, theme: Resources.Theme?): Drawable {
            throw UnsupportedOperationException("newDrawable(Resources,Theme) is not supported")
        }
    }

    // ----- Callbacks for child drawable -----
    private inner class ChildCallback : Callback {
        override fun invalidateDrawable(who: Drawable) {
            if (who === currentFrame()) invalidateSelf()
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, whenMillis: Long) {
            if (who === currentFrame()) scheduleSelf(what, whenMillis)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            if (who === currentFrame()) unscheduleSelf(what)
        }
    }

    private inner class ChildOnMotionEnd : OnMotionEndListener {
        override fun onMotionEnd(drawable: MotionDrawable) {
            if (drawable === currentFrame()) updateFrame()
        }
    }

    fun addFrameFromFile(skinDir: File, nameNoExt: String, durationMs: Int) {
        val base = nameNoExt.trim()
        val candidate = listOf("png", "webp", "jpg", "jpeg")
            .asSequence()
            .map { File(skinDir, "$base.$it") }
            .firstOrNull { it.exists() } ?: return

        val bmp = BitmapFactory.decodeFile(candidate.absolutePath) ?: return
        val dr = bmp.toDrawable(Resources.getSystem())
        addFrame(dr, durationMs.coerceAtLeast(0))
    }
}
