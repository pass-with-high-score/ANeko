package org.nqmgaming.aneko.core.motion

import kotlin.math.atan2

object MotionDirectionResolver {
    private val directions = arrayOf(
        MotionParams.MoveDirection.RIGHT,
        MotionParams.MoveDirection.DOWN_RIGHT,
        MotionParams.MoveDirection.DOWN,
        MotionParams.MoveDirection.DOWN_LEFT,
        MotionParams.MoveDirection.LEFT,
        MotionParams.MoveDirection.UP_LEFT,
        MotionParams.MoveDirection.UP,
        MotionParams.MoveDirection.UP_RIGHT,
    )

    fun resolve(dx: Float, dy: Float): MotionParams.MoveDirection? {
        if (dx == 0f && dy == 0f) return null
        val index = ((atan2(dy, dx) * 4 / Math.PI) + 8.5).toInt() % directions.size
        return directions[index]
    }
}
