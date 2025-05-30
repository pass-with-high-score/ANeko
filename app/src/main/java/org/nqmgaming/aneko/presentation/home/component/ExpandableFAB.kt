package org.nqmgaming.aneko.presentation.home.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Only Fan type, anchored at bottom-end (Right).
 */
@Composable
fun ExpandableFab(
    modifier: Modifier,
    isOpen: Boolean,
    onToggle: () -> Unit,
    distance: Dp = 100.dp,
    duration: Int = 300,
    fanAngle: Float = 90f,
    openIcon: @Composable () -> Unit = { Icon(Icons.Filled.Menu, null) },
    closeIcon: @Composable () -> Unit = { Icon(Icons.Filled.Close, null) },
    fabSize: Dp = 56.dp,
    children: List<@Composable () -> Unit>
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = isOpen
    val transition = rememberTransition(transitionState, label = "fabTransition")

    val factor by transition.animateFloat(
        transitionSpec = { tween(duration, easing = FastOutSlowInEasing) },
        label = "expandFactor"
    ) { state -> if (state) 1f else 0f }

    // Capture density and pixel distance once
    val density = LocalDensity.current
    val distancePx = with(density) { distance.toPx() }

    // Delay initial
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        ready = true
    }

    Box(modifier = modifier.alpha(if (ready) 1f else 0f)) {
        // Children fan layout
        val paddingEnd = fabSize / 2
        val paddingBottom = fabSize / 2
        Layout(
            content = { children.forEach { Box { it() } } },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = paddingEnd,
                    bottom = paddingBottom
                ),
        ) { measurable, constraints ->
            val placeable = measurable.map { it.measure(constraints) }
            layout(constraints.maxWidth, constraints.maxHeight) {
                // Anchor at screen bottom-end corner
                val anchorX = constraints.maxWidth.toFloat()
                val anchorY = constraints.maxHeight.toFloat()
                val step = if (placeable.size > 1) fanAngle / (placeable.size - 1) else 0f
                val startAngle = 180 + (90 - fanAngle) / 2

                placeable.forEachIndexed { i, p ->
                    val angleRad = Math.toRadians((startAngle + step * i).toDouble()).toFloat()
                    val dx = distancePx * factor * cos(angleRad)
                    val dy = distancePx * factor * sin(angleRad)

                    p.place(
                        x = (anchorX + dx - p.width / 2f).toInt(),
                        y = (anchorY + dy - p.height / 2f).toInt()
                    )
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = {
                // if is animating do nothing
                if (transitionState.currentState != transitionState.targetState) return@FloatingActionButton
                onToggle()
            },
            containerColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(),
            modifier = Modifier
                .size(fabSize)
                .align(Alignment.BottomEnd)
        ) {
            // Open icon
            Box(
                modifier = Modifier
                    .rotate(factor * 180f)
                    .scale(1f - factor)
            ) {
                openIcon()
            }
            // Close icon
            Box(
                modifier = Modifier
                    .rotate((1 - factor) * 180f)
                    .scale(factor)
            ) {
                closeIcon()
            }
        }
    }
}



