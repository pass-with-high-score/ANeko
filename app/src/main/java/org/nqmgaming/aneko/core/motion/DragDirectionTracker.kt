package org.nqmgaming.aneko.core.motion

import kotlin.math.hypot

class DragDirectionTracker {
    private var anchorX = 0f
    private var anchorY = 0f
    private var currentDirection: MotionParams.MoveDirection? = null

    fun reset(rawX: Float, rawY: Float) {
        anchorX = rawX
        anchorY = rawY
        currentDirection = null
    }

    fun update(
        rawX: Float,
        rawY: Float,
        activationThreshold: Float,
        directionChangeThreshold: Float,
    ): MotionParams.MoveDirection? {
        val dx = rawX - anchorX
        val dy = rawY - anchorY
        val threshold = if (currentDirection == null) {
            activationThreshold
        } else {
            directionChangeThreshold
        }.coerceAtLeast(0f)
        if (hypot(dx, dy) < threshold) return null

        anchorX = rawX
        anchorY = rawY
        val direction = MotionDirectionResolver.resolve(dx, dy) ?: return null
        if (direction == currentDirection) return null
        currentDirection = direction
        return direction
    }
}
