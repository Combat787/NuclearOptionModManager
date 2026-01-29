package com.combat.nomm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

object RepoMods {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    val mods: StateFlow<List<Extension>>
        field = MutableStateFlow<List<Extension>>(emptyList())

    val isLoading: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        fetchManifest()
    }

    fun fetchManifest(fake: Boolean = false) {
        scope.launch {
            mutex.withLock {
                isLoading.value = true
                try {
                    val url = "https://kopterbuzz.github.io/NOModManifestTesting/manifest/manifest.json"
                    val fetched = if (fake) fetchFakeManifest() else NetworkClient.fetchManifest(url)
                    if (fetched != null) {
                        mods.value = fetched
                    }
                } finally {
                    isLoading.value = false
                }
            }
        }
    }

    fun installMod(id: String, version: Version?, processing: MutableSet<String> = mutableSetOf()) {
        if (id in processing) return
        processing.add(id)

        val extension = mods.value.find { it.id == id } ?: return
        val artifact = version?.let { v -> extension.artifacts.find { it.version == v } }
            ?: extension.artifacts.maxByOrNull { it.version }
            ?: return

        artifact.dependencies.forEach { dep ->
            installMod(dep.id, dep.version, processing)
        }
        artifact.extends?.let { dep ->
            installMod(dep.id, dep.version, processing)
        }

        val bepinexFolder = File(SettingsManager.gameFolder, "BepInEx")
        val disabledPluginsFolder = File(bepinexFolder, "disabledPlugins")
        disabledPluginsFolder.mkdir()

        val dir = File(disabledPluginsFolder, id)
        dir.mkdir()

        Installer.installMod(
            extension.id,
            artifact.downloadUrl,
            dir,
        ) {
            val meta = File(dir, "meta.json")
            meta.createNewFile()
            meta.writeText(
                json.encodeToString(
                    ModMeta(
                        extension.id,
                        artifact,
                        extension,
                    )
                )
            )
            LocalMods.refresh()
            LocalMods.mods.value[extension.id]?.enable()
        }
    }
}