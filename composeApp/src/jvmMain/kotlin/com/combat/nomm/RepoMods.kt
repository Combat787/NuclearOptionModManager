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

    init {
        fetchManifest()
    }

    val mods: StateFlow<List<Extension>>
        field = MutableStateFlow<List<Extension>>(emptyList())

    val isLoading: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun fetchManifest(fake: Boolean = false) {
        scope.launch {
            mutex.withLock {
                isLoading.value = true
                try {
                    val fetched = if (fake) fetchFakeManifest() else json.decodeFromString<Manifest>(
                        """
                        [
                          {
                            "id": "KopterBuzz.NOFakeMod_1_FakeAddOn",
                            "displayName": "NOFakeMod_1_FakeAddOn",
                            "description": "NOFakeMod_1_FakeAddOn for Testing",
                            "tags": [
                              "fake"
                            ],
                            "infoUrl": "https://github.com/KopterBuzz/NOFakeMod_1",
                            "authors": [
                              "KopterBuzz"
                            ],
                            "artifacts": [
                              {
                                "fileName": "NOFakeMod_1_FakeAddOn-1.0.0.0.zip",
                                "version": "1.0.0.1",
                                "category": "release",
                                "type": "addon",
                                "gameVersion": "0.32",
                                "downloadUrl": "https://github.com/KopterBuzz/NOFakeMod_1/releases/download/1.0.0.0/NOBlackBox-1.0.0.0.zip",
                                "hash": "sha256:4a1b37d5b3cde5ac5b53b86ef85d7c9ad3fe1b88935021cf18eb62f99c69d6bf",
                                "extends": {
                                  "id": "KopterBuzz.NOFakeMod1",
                                  "version": "1.0.0.0"
                                }
                              }
                            ]
                          },
                          {
                            "id": "KopterBuzz.NOFakeMod1",
                            "displayName": "NOFakeMod 1",
                            "description": "FakeMod_1 for Testing",
                            "tags": [
                              "fake"
                            ],
                            "infoUrl": "https://github.com/KopterBuzz/NOFakeMod_1",
                            "authors": [
                              "KopterBuzz"
                            ],
                            "artifacts": [
                              {
                                "fileName": "NOFakeMod_1-1.0.0.0.zip",
                                "version": "1.0.0.3",
                                "category": "release",
                                "type": "mod",
                                "gameVersion": "0.32",
                                "downloadUrl": "https://github.com/KopterBuzz/NOFakeMod_1/releases/download/1.0.0.0/NOFakeMod_1-1.0.0.0.zip",
                                "hash": "sha256:4a1b37d5b3cde5ac5b53b86ef85d7c9ad3fe1b88935021cf18eb62f99c69d6bf"
                              }
                            ]
                          },
                          {
                            "id": "KopterBuzz.NOFakeMod2",
                            "displayName": "NOFakeMod 2",
                            "description": "FakeMod_2 for Testing",
                            "tags": [
                              "fake"
                            ],
                            "infoUrl": "https://github.com/KopterBuzz/NOFakeMod_2",
                            "authors": [
                              "KopterBuzz"
                            ],
                            "artifacts": [
                              {
                                "fileName": "NOFakeMod_2-1.0.0.0.zip",
                                "version": "1.0.0.4",
                                "category": "release",
                                "type": "mod",
                                "gameVersion": "0.32",
                                "downloadUrl": "https://github.com/KopterBuzz/NOFakeMod_1/releases/download/1.0.0.0/NOFakeMod_2_1.0.0.0.zip",
                                "hash": "sha256:4a1b37d5b3cde5ac5b53b86ef85d7c9ad3fe1b88935021cf18eb62f99c69d6bf",
                                "dependencies": [
                                  {
                                    "id": "KopterBuzz.NOFakeMod1",
                                    "version": "1.0.0.0"
                                  }
                                ]
                              }
                            ]
                          },
                          {
                            "id": "KopterBuzz.NOFakeMod3",
                            "displayName": "NOFakeMod 3",
                            "description": "FakeMod_3 for Testing",
                            "tags": [
                              "fake"
                            ],
                            "infoUrl": "https://github.com/KopterBuzz/NOFakeMod_3",
                            "authors": [
                              "KopterBuzz"
                            ],
                            "artifacts": [
                              {
                                "fileName": "NOFakeMod_3-1.0.0.0.zip",
                                "version": "1.0.0.5",
                                "category": "release",
                                "type": "mod",
                                "gameVersion": "0.32",
                                "downloadUrl": "https://github.com/KopterBuzz/NOFakeMod_3/releases/download/1.0.0.0/NOFakeMod_3-1.0.0.0.zip",
                                "hash": "sha256:4a1b37d5b3cde5ac5b53b86ef85d7c9ad3fe1b88935021cf18eb62f99c69d6bf",
                                "incompatibilities": [
                                  {
                                    "id": "KopterBuzz.NOFakeMod1",
                                    "version": "1.0.0.0"
                                  }
                                ]
                              }
                            ]
                          }
                        ]
                    """.trimIndent()
                    )// fetchManifest("https://kopterbuzz.github.io/NOModManifestTesting/manifest/manifest.json")
                    mods.value = fetched
                } finally {
                    isLoading.value = false
                }
                LocalMods.mods.value
            }
        }
    }


    fun installMod(id: String, version: Version?) {
        val extension = mods.value.find { it.id == id }
        if (extension == null) {
            return
        }
        val artifact =
            if (version != null) extension.artifacts.first { it.version == version } else extension.artifacts.maxBy { it.version }
        
        artifact.dependencies.forEach { 
            installMod(it.id, it.version)
        }
        
        val disabledPluginsFolder = File(SettingsManager.gameFolder, "disabledPlugins")
        disabledPluginsFolder.mkdir()
        val dir = File(disabledPluginsFolder,id)
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
            LocalMods.mods.value[extension.id]?.enable()
        }
    }
}