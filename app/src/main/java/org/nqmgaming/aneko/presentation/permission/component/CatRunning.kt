package org.nqmgaming.aneko.presentation.permission.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.nqmgaming.aneko.R


@Composable
fun CatRunning() {
    val action = remember {
        listOf(CatAction.RIGHT, CatAction.KAKI).random()
    }


    val frames = remember(action) {
        when (action) {
            CatAction.RIGHT -> listOf(
                R.drawable.right1,
                R.drawable.right2
            )

            CatAction.KAKI -> listOf(
                R.drawable.kaki1,
                R.drawable.kaki2
            )
        }
    }


    var frameIndex by remember { mutableIntStateOf(0) }


    LaunchedEffect(frames) {
        while (true) {
            frameIndex = (frameIndex + 1) % frames.size
            kotlinx.coroutines.delay(200)
        }
    }


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = frames[frameIndex],
            contentDescription = "Cat ${action.name.lowercase()}",
            modifier = Modifier.size(48.dp)
        )


        Spacer(Modifier.height(12.dp))


        Text(
            text = stringResource(R.string.notification_enabled),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}