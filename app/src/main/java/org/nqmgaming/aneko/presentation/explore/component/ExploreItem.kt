package org.nqmgaming.aneko.presentation.explore.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.download.DownloadStatus
import org.nqmgaming.aneko.core.download.DownloadTask
import org.nqmgaming.aneko.core.download.SkinDownloadQueue
import org.nqmgaming.aneko.data.SkinCollection

@Composable
fun ExploreItem(
    collection: SkinCollection,
    isInstalled: Boolean,
    st: DownloadStatus,
    queuePos: Int?,
    onUninstall: (() -> Unit)? = null,
    localVersion: String = "",
) {
    val context = LocalContext.current

    val hasUpdate = isInstalled && localVersion.isNotBlank() &&
            collection.version.isNotBlank() && localVersion < collection.version

    val label = when (st) {
        is DownloadStatus.Idle -> when {
            hasUpdate -> stringResource(R.string.update)
            isInstalled -> stringResource(R.string.installed)
            else -> stringResource(R.string.download)
        }

        is DownloadStatus.Queued -> "${stringResource(R.string.queued)} #$queuePos"

        is DownloadStatus.Downloading -> "${stringResource(R.string.downloading)}  ${st.progressPct}%"

        is DownloadStatus.Importing -> stringResource(R.string.importing)
        is DownloadStatus.Done -> stringResource(R.string.installed)
        is DownloadStatus.Failed -> stringResource(R.string.retry)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(collection.image)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(
                            shape = RoundedCornerShape(12.dp),
                        )
                )
                Spacer(modifier = Modifier.size(16.dp))
                Column(modifier = Modifier.weight(4f)) {
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = collection.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "by @" + (collection.author
                            ?: stringResource(R.string.unknown_author)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (collection.version.isNotBlank()) {
                        Text(
                            text = "v${collection.version}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        when (st) {
                            is DownloadStatus.Idle,
                            is DownloadStatus.Failed,
                            DownloadStatus.Done -> {
                                SkinDownloadQueue.enqueue(
                                    context = context,
                                    task = DownloadTask(
                                        id = collection.packageName,
                                        url = collection.url,
                                        fileName = "${collection.packageName}.zip"
                                    )
                                )
                            }

                            is DownloadStatus.Queued -> {
                                SkinDownloadQueue.cancel(collection.packageName)
                            }

                            is DownloadStatus.Downloading,
                            DownloadStatus.Importing -> {
                            }
                        }
                    },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (hasUpdate) Icons.Default.Update else Icons.Default.Download,
                            contentDescription = label,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (isInstalled && onUninstall != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onUninstall,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.uninstall),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = stringResource(R.string.uninstall),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}