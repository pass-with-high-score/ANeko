package org.nqmgaming.aneko.core.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MotionDirectionResolverTest {
    @Test
    fun `resolves all drag directions`() {
        val directions = mapOf(
            1f to 0f to MotionParams.MoveDirection.RIGHT,
            1f to 1f to MotionParams.MoveDirection.DOWN_RIGHT,
            0f to 1f to MotionParams.MoveDirection.DOWN,
            -1f to 1f to MotionParams.MoveDirection.DOWN_LEFT,
            -1f to 0f to MotionParams.MoveDirection.LEFT,
            -1f to -1f to MotionParams.MoveDirection.UP_LEFT,
            0f to -1f to MotionParams.MoveDirection.UP,
            1f to -1f to MotionParams.MoveDirection.UP_RIGHT,
        )

        directions.forEach { (vector, direction) ->
            assertEquals(direction, MotionDirectionResolver.resolve(vector.first, vector.second))
        }
    }

    @Test
    fun `returns null when drag has no displacement`() {
        assertNull(MotionDirectionResolver.resolve(0f, 0f))
    }
}
