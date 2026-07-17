package org.nqmgaming.aneko.core.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DragDirectionTrackerTest {
    @Test
    fun `ignores jitter until a direction threshold is crossed`() {
        val tracker = DragDirectionTracker().apply { reset(100f, 100f) }

        assertNull(tracker.update(103f, 100f, 4f, 20f))
        assertEquals(
            MotionParams.MoveDirection.RIGHT,
            tracker.update(105f, 100f, 4f, 20f),
        )
        assertNull(tracker.update(99f, 101f, 4f, 20f))
        assertNull(tracker.update(111f, 98f, 4f, 20f))
        assertEquals(
            MotionParams.MoveDirection.LEFT,
            tracker.update(80f, 100f, 4f, 20f),
        )
    }

    @Test
    fun `does not restart animation while direction stays the same`() {
        val tracker = DragDirectionTracker().apply { reset(0f, 0f) }

        assertEquals(
            MotionParams.MoveDirection.DOWN_RIGHT,
            tracker.update(5f, 5f, 4f, 10f),
        )
        assertNull(tracker.update(15f, 15f, 4f, 10f))
    }
}
