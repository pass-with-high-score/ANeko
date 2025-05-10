package org.nqmgaming.aneko.presentation.home.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun SmallFab(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isExpanded: Boolean,
) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .zIndex(2f)
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .size(animateDpAsState(targetValue = if (isExpanded) 60.dp else 40.dp).value)
                .shadow(
                    animateDpAsState(targetValue = if (isExpanded) 10.dp else 0.dp).value,
                    RoundedCornerShape(50)
                ),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            // Small FAB icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(8.dp)
            ) {

                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    ),
                    exit = fadeOut(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    ),
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}