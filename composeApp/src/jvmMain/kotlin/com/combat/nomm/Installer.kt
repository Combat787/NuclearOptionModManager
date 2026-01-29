package com.combat.nomm

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

object Installer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val locks = ConcurrentHashMap<String, Mutex>()

    val bepinexStatus = MutableStateFlow<TaskState?>(null)
    val installStatuses = MutableStateFlow<Map<String, TaskState>>(emptyMap())

    fun installBepinex(gameDir: File) {
        val url = "https://github.com/BepInEx/BepInEx/releases/download/v5.4.23.4/BepInEx_win_x64_5.4.23.4.zip"
        installMod("bepinex", url, gameDir, true) {}
    }

    fun installMod(modId: String, url: String, dir: File, isBepInEx: Boolean = false, onSuccess: () -> Unit) {
        lateinit var job: Job
        job = scope.launch {
            val mutex = locks.getOrPut(modId) { Mutex() }
            if (!mutex.tryLock()) return@launch

            try {
                executeDownloadAndExtract(url, dir, { job.cancel() }) { state ->
                    updateState(modId, state, isBepInEx)
                }
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                clearStatus(modId, isBepInEx)
                mutex.unlock()
            }
        }
    }

    private suspend fun executeDownloadAndExtract(
        url: String,
        targetDir: File,
        cancelAction: () -> Unit,
        onUpdate: (TaskState) -> Unit
    ) {
        val response = NetworkClient.client.get(url) {
            onDownload { sent, total ->
                val progress = if ((total ?: 0L) > 0) sent.toFloat() / total!! else null
                onUpdate(TaskState(TaskState.Phase.DOWNLOADING, progress, onCancel = cancelAction))
            }
        }

        yield()
        onUpdate(TaskState(TaskState.Phase.EXTRACTING, null, isCancellable = false))

        withContext(Dispatchers.IO) {
            response.bodyAsChannel().toInputStream().use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val outFile = File(targetDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { zip.copyTo(it) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
        }
    }

    private fun updateState(modId: String, state: TaskState, isBepInEx: Boolean) {
        if (isBepInEx) bepinexStatus.value = state
        else installStatuses.update { it + (modId to state) }
    }

    private fun clearStatus(modId: String, isBepInEx: Boolean) {
        if (isBepInEx) {
            bepinexStatus.value = null
        } else {
            installStatuses.update { it - modId }
        }
    }
}

data class TaskState(
    val phase: Phase,
    val progress: Float? = 0f,
    val error: String? = null,
    val isCancellable: Boolean = true,
    private val onCancel: (() -> Unit)? = null
) {
    enum class Phase { DOWNLOADING, EXTRACTING }

    fun cancel() {
        if (isCancellable) onCancel?.invoke()
    }
}