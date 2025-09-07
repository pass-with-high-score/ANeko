package org.nqmgaming.aneko.core.download

data class DownloadTask(
    val id: String,
    val url: String,
    val fileName: String
)

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Queued(val position: Int) : DownloadStatus()
    data class Downloading(val progressPct: Int) : DownloadStatus()
    object Importing : DownloadStatus()
    object Done : DownloadStatus()
    data class Failed(val message: String) : DownloadStatus()
}