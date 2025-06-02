package org.nqmgaming.aneko.presentation.home.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.data.skin.SkinConfig
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinCard(
    skin: SkinConfig,
    isSelected: Boolean,
    onSkinSelected: () -> Unit,
    onRequestDeleteSkin: () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val bottomSheetState = rememberModalBottomSheetState()
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val defaultPackageName = context.packageName
    val isDefaultSkin = skin.info.skinId == defaultPackageName
    val skinDir = File(context.filesDir, "skins/${skin.info.skinId}")
    val imageFile = File(skinDir, skin.info.icon)
    val bitmap = remember(imageFile.path) {
        BitmapFactory.decodeFile(imageFile.absolutePath)?.asImageBitmap()
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
                .padding(bottom = 16.dp, top = 4.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${skin.info.name} ${if (isDefaultSkin) stringResource(R.string.default_label) else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isDefaultSkin) FontWeight.Bold else null,
            )
        }
    }

    SkinDetailsBottomSheet(
        skin = skin.info,
        onDismissRequest = {
            isBottomSheetVisible = false
        },
        onRequestDeleteSkin = onRequestDeleteSkin,
        isBottomSheetVisible = isBottomSheetVisible,
        bottomSheetState = bottomSheetState,
    )
}