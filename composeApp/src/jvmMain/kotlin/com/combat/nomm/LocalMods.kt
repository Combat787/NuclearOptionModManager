package com.combat.nomm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object LocalMods {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    val mods: StateFlow<Map<String, ModMeta>>
        field = MutableStateFlow(loadInstalledModMetas(File(SettingsManager.gameFolder, "BepInEx")))

    fun loadInstalledModMetas(bepinexFolder: File): Map<String, ModMeta> {

        bepinexFolder.mkdir()
        if (!bepinexFolder.exists() || !bepinexFolder.isDirectory) return emptyMap()
        val pluginsFolder = File(bepinexFolder, "plugins")
        val disabledPlugins = File(bepinexFolder, "disabledPlugins")

        pluginsFolder.mkdir()
        disabledPlugins.mkdir()

        fun load(rootFolder: File, enabled: Boolean): Map<String, ModMeta> {

            return rootFolder.listFiles { file -> file.isDirectory }
                ?.associate { folder ->
                    val jsonFile = File(folder, "meta.json")

                    val modMeta = if (jsonFile.exists()) {
                        json.decodeFromString<ModMeta>(jsonFile.readText())
                    } else {
                        val meta =
                            ModMeta(id = folder.name, artifact = null, cachedExtension = null)
                        File(folder, "meta.json").writeText(
                            json.encodeToString(meta)
                        )
                        meta
                    }
                    modMeta.file = folder
                    modMeta.enabled = enabled
                    modMeta.id to modMeta
                } ?: emptyMap()
        }

        return load(pluginsFolder, true) + load(disabledPlugins, false)
    }
}

@Serializable
data class ModMeta(
    var id: String,
    var artifact: Artifact?,
    var cachedExtension: Extension?,
    @Transient var enabled: Boolean? = null,
    @Transient var file: File? = null,
) {
    fun enable() {
        if (artifact?.extends != null) {
            artifact!!.extends!!.id
            LocalMods.mods.value[id]?.enable()
        }
        artifact?.dependencies?.forEach {
            it.id
            LocalMods.mods.value[id]?.enable()
        }
        if (enabled == null) {
            enabled = (file!!.parent != "disabledPlugins")
        }
        if (!enabled!!) {
            val destination = File(SettingsManager.bepInExFolder, "plugins")
            file?.moveTo(destination)
            file = destination
        }
    }
    
    fun disable() {
        if (enabled == null) {
            enabled = (file!!.parent == "plugins")
        }
        if (enabled!!) {
            val destination = File(SettingsManager.bepInExFolder, "disabledPlugins")
            file?.moveTo(destination)
            file = destination
        }
    }

    fun uninstall() {
        TODO("Not yet implemented")
    }

    fun update() {
        TODO("Not yet implemented")
    }
}

fun File.moveTo(destination: File) {
    runCatching {
        Files.move(
            toPath(),
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}