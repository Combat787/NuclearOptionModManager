package com.combat.nomm

import com.github.junrar.Archive
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

object Installer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val locks = ConcurrentHashMap<String, Mutex>()

    val bepinexStatus: StateFlow<TaskState?>
        field = MutableStateFlow<TaskState?>(null)

    val installStatuses: StateFlow<Map<String, TaskState>>
        field = MutableStateFlow<Map<String, TaskState>>(emptyMap())

    @Suppress("unused")
    fun installBepinex(gameDir: File) {
        val url = "https://github.com/BepInEx/BepInEx/releases/download/v5.4.23.4/BepInEx_win_x64_5.4.23.4.zip"
        installMod("bepinex", url, gameDir, true) {}
    }

    fun installMod(modId: String, url: String, dir: File, isBepInEx: Boolean = false, onSuccess: () -> Unit) {
        scope.launch {
            val mutex = locks.getOrPut(modId) { Mutex() }
            if (!mutex.tryLock()) return@launch

            try {
                val bytes = downloadFile(url, modId, isBepInEx) { this.cancel() }

                yield()
                updateState(modId, TaskState(TaskState.Phase.EXTRACTING, null, isCancellable = false), isBepInEx)

                extract(bytes, url, dir)
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState(modId, TaskState(TaskState.Phase.EXTRACTING, error = e.localizedMessage), isBepInEx)
            } finally {
                clearStatus(modId, isBepInEx)
                mutex.unlock()
            }
        }
    }

    private suspend fun downloadFile(url: String, modId: String, isBepInEx: Boolean, onCancel: () -> Unit): ByteArray {
        val response = NetworkClient.client.get(url) {
            onDownload { sent, total ->
                val progress = if ((total ?: 0L) > 0) sent.toFloat() / total!! else null
                updateState(modId, TaskState(TaskState.Phase.DOWNLOADING, progress, onCancel = onCancel), isBepInEx)
            }
        }
        return response.readRawBytes()
    }

    private fun extract(bytes: ByteArray, url: String, targetDir: File) {
        val fileName = url.substringAfterLast("/")
        val extension = fileName.substringAfterLast(".", "").lowercase()

        when (extension) {
            "zip" -> extractZip(ByteArrayInputStream(bytes), targetDir)
            "7z" -> extract7z(bytes, targetDir)
            "rar" -> extractRar(ByteArrayInputStream(bytes), targetDir)
            else -> File(targetDir, fileName).writeBytes(bytes)
        }
    }

    private fun extractZip(input: InputStream, targetDir: File) {
        ZipInputStream(input).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdir()
                } else {
                    outFile.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
            }
        }
    }

    private fun extract7z(bytes: ByteArray, targetDir: File) {
        SevenZFile.builder()
            .setSeekableByteChannel(SeekableInMemoryByteChannel(bytes))
            .get().use { archive ->
                generateSequence { archive.nextEntry }.forEach { entry ->
                    val outFile = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdir()
                    } else {
                        outFile.outputStream().use { out ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (archive.read(buffer).also { len = it } != -1) {
                                out.write(buffer, 0, len)
                            }
                        }
                    }
                }
            }
    }

    private fun extractRar(input: InputStream, targetDir: File) {
        Archive(input).use { archive ->
            generateSequence { archive.nextFileHeader() }.forEach { entry ->
                val outFile = File(targetDir, entry.fileName)
                if (entry.isDirectory) {
                    outFile.mkdir()
                } else {
                    outFile.outputStream().use { out ->
                        archive.extractFile(entry, out)
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
        if (isBepInEx) bepinexStatus.value = null
        else installStatuses.update { it - modId }
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