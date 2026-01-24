package org.nqmgaming.aneko.presentation.explore.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
) {
    val context = LocalContext.current

    Column {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(collection.image)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(60.dp)
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
                    text = collection.author
                        ?: stringResource(R.string.unknown_author),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            Button(
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = stringResource(R.string.download),
                    )
                    val label = when (st) {
                        is DownloadStatus.Idle -> if (isInstalled)
                            stringResource(R.string.installed)
                        else stringResource(R.string.download)

                        is DownloadStatus.Queued -> "${stringResource(R.string.queued)} #$queuePos"

                        is DownloadStatus.Downloading -> "${stringResource(R.string.downloading)}  ${st.progressPct}%"

                        is DownloadStatus.Importing -> stringResource(R.string.importing)
                        is DownloadStatus.Done -> stringResource(R.string.installed)
                        is DownloadStatus.Failed -> stringResource(R.string.retry)
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline
        )
    }
}