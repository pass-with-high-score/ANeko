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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.FilterQuality
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
import kotlin.math.abs

private val collectibleCardColors = listOf(
    Color(0xFF26A69A), // Teal
    Color(0xFF5C6BC0), // Indigo
    Color(0xFFAB47BC), // Purple
    Color(0xFFEC407A), // Pink
    Color(0xFFEF5350), // Red
    Color(0xFF42A5F5), // Blue
    Color(0xFFFF7043), // Deep Orange
    Color(0xFF66BB6A), // Green
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinCard(
    skin: SkinEntity,
    isSelected: Boolean,
    onSkinSelect: () -> Unit,
    onRequestDeleteSkin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState()
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    val isDefaultSkin = skin.isBuiltin
    val model = remember(skin.packageName, skin.previewPath) {
        ImageRequest.Builder(context)
            .data(skin.previewModel(context))
            .crossfade(true)
            .build()
    }

    val cardColor = remember(skin.packageName) {
        collectibleCardColors[abs(skin.packageName.hashCode()) % collectibleCardColors.size]
    }
    val selectionBorder = if (isSelected) {
        Modifier.border(
            width = 4.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(20.dp),
        )
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(180.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .then(selectionBorder),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            onClick = onSkinSelect,
        ) {
            // Header: branding + menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ANeko",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🐾", style = MaterialTheme.typography.labelSmall)
                    if (!isDefaultSkin) {
                        IconButton(
                            onClick = { isBottomSheetVisible = !isBottomSheetVisible },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            // Inner frame with pet preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 3.dp,
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .background(cardColor.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                // Light inner area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = model,
                        contentDescription = skin.name,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.size(90.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }

        // Name below card
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${skin.name} ${if (isDefaultSkin) stringResource(R.string.default_label) else ""}".trim(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    SkinDetailsBottomSheet(
        skin = skin,
        onDismissRequest = { isBottomSheetVisible = false },
        onRequestDeleteSkin = onRequestDeleteSkin,
        isBottomSheetVisible = isBottomSheetVisible,
        bottomSheetState = bottomSheetState,
    )
}