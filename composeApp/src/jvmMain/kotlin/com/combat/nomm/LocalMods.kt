package com.combat.nomm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object LocalMods {
    private val _mods = MutableStateFlow(loadInstalledModMetas(File(SettingsManager.gameFolder, "BepInEx")))
    val mods: StateFlow<Map<String, ModMeta>> = _mods

    fun loadInstalledModMetas(bepinexFolder: File): Map<String, ModMeta> {
        if (!bepinexFolder.exists()) return emptyMap()

        val plugins = File(bepinexFolder, "plugins").apply { mkdir() }
        val disabled = File(bepinexFolder, "disabledPlugins").apply { mkdir() }
        val foundMods = mutableMapOf<String, ModMeta>()

        fun scanFolder(root: File, isEnabled: Boolean) {
            root.listFiles { it.isDirectory }?.forEach { folder ->
                val jsonFile = File(folder, "meta.json")
                if (jsonFile.exists()) {
                    val meta = runCatching { RepoMods.json.decodeFromString<ModMeta>(jsonFile.readText()) }.getOrNull()
                        ?: ModMeta(folder.name, null, null)

                    foundMods[meta.id] = meta.copy(file = folder, enabled = isEnabled)

                    val addonFolder = File(folder, "addons")
                    if (addonFolder.exists()) scanFolder(addonFolder, isEnabled)
                }
            }
        }

        scanFolder(plugins, true)
        scanFolder(disabled, false)
        return foundMods
    }

    fun updateModState(id: String, meta: ModMeta?) {
        _mods.update { current ->
            val newMap = current.toMutableMap()
            if (meta == null) newMap.remove(id) else newMap[id] = meta
            newMap
        }
    }

    fun refresh() {
        _mods.value = loadInstalledModMetas(File(SettingsManager.gameFolder, "BepInEx"))
    }
}

@Serializable
data class ModMeta(
    val id: String,
    val artifact: Artifact?,
    val cachedExtension: Extension?,
    @Transient val enabled: Boolean? = null,
    @Transient val file: File? = null,
) {
    fun enable() {
        val currentSelf = LocalMods.mods.value[id] ?: this
        if (currentSelf.enabled == true) return

        val parentId = artifact?.extends?.id
        val parentMod = parentId?.let { LocalMods.mods.value[it] }
        val newFile: File

        if (parentMod != null) {
            parentMod.enable()

            val updatedParent = LocalMods.mods.value[parentId]
            val addonDir = File(updatedParent?.file ?: parentMod.file, "addons").apply { mkdir() }

            newFile = File(addonDir, id)
            file?.moveTo(newFile)
        } else {
            newFile = File(SettingsManager.bepInExFolder, "plugins/$id")
            file?.moveTo(newFile)
        }

        val updatedMeta = this.copy(file = newFile, enabled = true)
        LocalMods.updateModState(id, updatedMeta)

        artifact?.dependencies?.forEach { dep ->
            LocalMods.mods.value[dep.id]?.enable()
        }
    }

    fun disable() {
        val currentSelf = LocalMods.mods.value[id] ?: this
        if (currentSelf.enabled == false) return

        val addonFolder = File(file, "addons")
        if (addonFolder.exists()) {
            addonFolder.listFiles { it.isDirectory }?.forEach { folder ->
                LocalMods.mods.value[folder.name]?.disable()
            }
        }

        val destination = File(SettingsManager.bepInExFolder, "disabledPlugins/$id")
        file?.moveTo(destination)

        val updatedMeta = this.copy(file = destination, enabled = false)
        LocalMods.updateModState(id, updatedMeta)
    }

    fun uninstall() {
        file?.deleteRecursively()
        LocalMods.updateModState(id, null)
    }
    
    fun update() {}
}

fun File.moveTo(destination: File) {
    runCatching {
        destination.parentFile?.mkdir()
        Files.move(
            toPath(),
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}