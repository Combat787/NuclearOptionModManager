package com.combat.nomm

import kotlinx.serialization.json.Json
import java.io.File

object DataStorage {
    val osName = System.getProperty("os.name").lowercase()
    val configPath = when {
        osName.contains("win") -> File(System.getenv("AppData"), appName)
        osName.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/$appName")
        else -> File(System.getProperty("user.home"), ".config/$appName")
    }
    val configFile = File(configPath, "config.json")

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
    }

    init {
        if (!configPath.exists()) configPath.mkdirs()
    }
}