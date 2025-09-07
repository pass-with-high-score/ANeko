package org.nqmgaming.aneko.core.download

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

object SkinDownloadQueue {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _pending = MutableStateFlow<List<DownloadTask>>(emptyList())
    val pending: StateFlow<List<DownloadTask>> = _pending.asStateFlow()

    private val _status = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val status: StateFlow<Map<String, DownloadStatus>> = _status.asStateFlow()

    private val processing = AtomicBoolean(false)

    private lateinit var ok: OkHttpClient
    fun ensureClient(context: Context) {
        if (this::ok.isInitialized) return
        val cacheDir = File(context.cacheDir, "http-skin")
        ok = OkHttpClient.Builder()
            .cache(Cache(cacheDir, 50L * 1024 * 1024))
            .build()
    }

    fun getStatus(id: String): DownloadStatus =
        _status.value[id] ?: DownloadStatus.Idle

    fun queuePositionOf(id: String): Int? =
        _pending.value.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.plus(1)

    fun enqueue(context: Context, task: DownloadTask) {
        ensureClient(context)
        if (_pending.value.any { it.id == task.id } ||
            (_status.value[task.id] is DownloadStatus.Downloading) ||
            (_status.value[task.id] is DownloadStatus.Importing)
        ) return

        _pending.update { it + task }
        recomputeQueued()

        if (processing.compareAndSet(false, true)) {
            scope.launch { processLoop(context) }
        }
    }

    fun cancel(taskId: String) {
        val before = _pending.value
        val after = before.filterNot { it.id == taskId }
        if (before.size != after.size) {
            _pending.value = after
            _status.update { it + (taskId to DownloadStatus.Idle) }
            recomputeQueued()
        }
    }

    private suspend fun processLoop(context: Context) {
        try {
            while (_pending.value.isNotEmpty()) {
                val task = _pending.value.first()

                _status.update { it + (task.id to DownloadStatus.Downloading(progressPct = 0)) }

                val result = runCatching {
                    downloadToExternalDownloads(context, task) { pct ->
                        _status.update { it + (task.id to DownloadStatus.Downloading(pct)) }
                    }
                }

                if (result.isSuccess) {
                    _status.update { it + (task.id to DownloadStatus.Importing) }
                    withContext(Dispatchers.Main) {
                        onImported?.invoke(task.id, result.getOrThrow())
                    }
                    _status.update { it + (task.id to DownloadStatus.Done) }
                } else {
                    _status.update {
                        it + (task.id to DownloadStatus.Failed(
                            result.exceptionOrNull()?.message ?: "Failed"
                        ))
                    }
                }

                _pending.update { it.drop(1) }
                recomputeQueued()
            }
        } finally {
            processing.set(false)
        }
    }

    private fun recomputeQueued() {
        val map = _status.value.toMutableMap()
        _pending.value.forEachIndexed { idx, t ->
            map[t.id] = DownloadStatus.Queued(position = idx + 1)
        }
        _status.value = map
    }

    private fun downloadToExternalDownloads(
        context: Context,
        task: DownloadTask,
        onProgress: (Int) -> Unit
    ): String {
        val req = Request.Builder().url(task.url).build()
        ok.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")

            val body = resp.body ?: error("Empty body")
            val total = body.contentLength().takeIf { it > 0 } ?: -1L

            val outFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "${task.fileName}.zip"
            )
            body.byteStream().use { input ->
                FileOutputStream(outFile).use { out ->
                    val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    var sum = 0L
                    var lastPct = 0
                    while (input.read(buf).also { read = it } >= 0) {
                        out.write(buf, 0, read)
                        sum += read
                        if (total > 0) {
                            val pct = min(100, ((sum * 100) / total).toInt())
                            if (pct != lastPct) {
                                lastPct = pct
                                onProgress(pct)
                            }
                        }
                    }
                }
            }
            return File(outFile.absolutePath).toURI().toString()
        }
    }

    @Volatile
    var onImported: ((taskId: String, fileUriString: String) -> Unit)? = null
}
