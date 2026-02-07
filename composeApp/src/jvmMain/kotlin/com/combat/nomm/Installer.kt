package com.combat.nomm

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import net.sf.sevenzipjbinding.*
import net.sf.sevenzipjbinding.util.ByteArrayStream
import java.io.FileOutputStream

object Installer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val locks = ConcurrentHashMap<String, Mutex>()

    val bepinexStatus: StateFlow<TaskState?>
        field = MutableStateFlow<TaskState?>(null)

    val installStatuses: StateFlow<Map<String, TaskState>>
        field = MutableStateFlow<Map<String, TaskState>>(emptyMap())

    fun installMod(modId: String, url: String, dir: File, isBepInEx: Boolean = false, onSuccess: () -> Unit) {

        updateState(modId, TaskState(TaskState.Phase.DOWNLOADING, 0f, null, false), isBepInEx)

        scope.launch {
            val mutex = locks.getOrPut(modId) { Mutex() }
            mutex.withLock {
                val job = coroutineContext[Job]
                try {
                    val bytes = downloadWithRetry(job, url, modId, isBepInEx)
                    updateState(modId, TaskState(TaskState.Phase.EXTRACTING, null, null, false), isBepInEx)

                    withContext(Dispatchers.IO) {
                        if (!dir.exists()) dir.mkdirs()
                        extract(bytes, url, dir, isBepInEx)
                    }
                    onSuccess()
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (e is CancellationException) throw e
                    updateState(modId, TaskState(TaskState.Phase.EXTRACTING, null, e.localizedMessage), isBepInEx)
                } finally {
                    clearStatus(modId, isBepInEx)
                }
            }
        }
    }

    private suspend fun downloadWithRetry(
        parentJob: Job?,
        url: String,
        modId: String,
        isBepInEx: Boolean,
        attempts: Int = 3,
    ): ByteArray {
        var lastErr: Exception? = null
        repeat(attempts) { i ->
            try {
                return NetworkClient.client.get(url) {
                    onDownload { sent, total ->
                        val p = if ((total ?: 0L) > 0) sent.toFloat() / total!! else null
                        updateState(
                            modId,
                            TaskState(TaskState.Phase.DOWNLOADING, p, null, true) { parentJob?.cancel() },
                            isBepInEx
                        )
                    }
                }.readRawBytes()
            } catch (e: Exception) {
                lastErr = e
                if (i < attempts - 1) delay(1000L * (i + 1))
            }
        }
        throw lastErr ?: Exception("Failed to download")
    }
    
    private fun extract(bytes: ByteArray, url: String, target: File, noOverwrite: Boolean) {
        runCatching {
            ByteArrayStream(bytes, false).use { inStream ->

                val archive = try {
                    SevenZip.openInArchive(null, inStream)
                } catch (_: SevenZipException) {
                    null
                }

                if (archive != null) {
                    archive.use { arc ->
                        arc.simpleInterface.archiveItems.forEach { item ->
                            val file = File(target, item.path)
                            if (item.isFolder) {
                                file.mkdirs()
                            } else {
                                if (!noOverwrite || !file.exists()) {
                                    file.parentFile?.mkdirs()
                                    FileOutputStream(file).use { out ->
                                        item.extractSlow { data ->
                                            out.write(data)
                                            data.size
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val file = File(target, url.substringAfterLast("/"))
                    if (!noOverwrite || !file.exists()) {
                        file.parentFile?.mkdirs()
                        file.writeBytes(bytes)
                    }
                }
            }
        }.onFailure {
            if (!noOverwrite) target.deleteRecursively()
            throw it
        }
    }


    private fun updateState(id: String, state: TaskState, isBep: Boolean) {
        if (isBep) {
            bepinexStatus.value = state
        } else {
            installStatuses.update { it + (id to state) }
        }
    }

    private fun clearStatus(id: String, isBep: Boolean) {
        if (isBep) {
            bepinexStatus.value = null
        } else {
            installStatuses.update { it - id }
        }

    }
}

data class TaskState(
    val phase: Phase,
    val progress: Float? = 0f,
    val error: String? = null,
    val isCancellable: Boolean = true,
    private val onCancel: (() -> Unit)? = null,
) {
    enum class Phase { DOWNLOADING, EXTRACTING }

    fun cancel() {
        if (isCancellable) onCancel?.invoke()
    }
}