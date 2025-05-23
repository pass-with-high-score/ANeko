package org.nqmgaming.aneko.util.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private data class ThrottleTimer(
    private val interval: Long = 500L,
    private var value: Long = 0L
) {
    fun expired(): Boolean {
        val t = System.currentTimeMillis()
        if (t - value > interval) {
            value = t
            return true
        }
        return false
    }
}

private val defaultThrottleTimer by lazy { ThrottleTimer() }

@Composable
fun throttle(
    fn: (() -> Unit),
): (() -> Unit) {
    return remember(fn) {
        {
            if (defaultThrottleTimer.expired()) {
                fn.invoke()
            }
        }
    }
}