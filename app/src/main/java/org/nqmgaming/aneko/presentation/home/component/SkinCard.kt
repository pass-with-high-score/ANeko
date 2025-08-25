package org.nqmgaming.aneko.presentation.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.data.entity.SkinEntity
import org.nqmgaming.aneko.core.data.entity.previewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinCard(
    skin: SkinEntity,
    isSelected: Boolean,
    onSkinSelected: () -> Unit,
    onRequestDeleteSkin: () -> Unit,
) {
    val context = LocalContext.current
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val bottomSheetState = rememberModalBottomSheetState()
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    val isDefaultSkin = skin.isBuiltin
    val model = remember(skin.packageName, skin.previewPath) {
        ImageRequest.Builder(context)
            .data(skin.previewModel(context))
            .crossfade(true)
            .build()
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.large
            ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onSkinSelected
    ) {

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    isBottomSheetVisible = !isBottomSheetVisible
                }
            ) {
                if (!isDefaultSkin) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Color.White,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(80.dp)
                        .background(
                            Color.White,
                        ),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${skin.name} ${if (isDefaultSkin) stringResource(R.string.default_label) else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isDefaultSkin) FontWeight.Bold else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    SkinDetailsBottomSheet(
        skin = skin,
        onDismissRequest = {
            isBottomSheetVisible = false
        },
        onRequestDeleteSkin = onRequestDeleteSkin,
        isBottomSheetVisible = isBottomSheetVisible,
        bottomSheetState = bottomSheetState,
    )
}