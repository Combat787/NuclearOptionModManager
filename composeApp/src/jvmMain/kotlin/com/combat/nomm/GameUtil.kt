package com.combat.nomm

import java.io.File

fun getGameFolder(folderName: String, executableName: String): File? {
    val steamPath = getSteamPath() ?: return null
    val vdf = File(steamPath, "steamapps/libraryfolders.vdf").let {
        if (it.exists()) it else File(steamPath, "config/libraryfolders.vdf")
    }
    if (!vdf.exists()) return null

    val libs = "\"path\"\\s+\"(.+?)\"".toRegex().findAll(vdf.readText())
        .map { File(it.groupValues[1].replace("\\\\", "/")) }
        .plus(File(steamPath))
        .distinct()

    return libs.firstNotNullOfOrNull { lib ->
        val gameDir = File(lib, "steamapps/common/$folderName")
        val exeFile = File(gameDir, executableName)
        if (exeFile.exists()) gameDir else null
    }
}
fun getSteamPath(): String? {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")

    return when {
        os.contains("win") -> {
            val pb = ProcessBuilder("reg", "query", "HKCU\\Software\\Valve\\Steam", "/v", "SteamPath")
            val out = try { pb.start().inputStream.bufferedReader().use { it.readText() } } catch (_: Exception) { "" }
            "SteamPath\\s+REG_SZ\\s+(.*)".toRegex().find(out)?.groupValues?.get(1)?.trim()
        }
        os.contains("mac") -> "$home/Library/Application Support/Steam"
        else -> listOf("$home/.local/share/Steam", "$home/.steam/steam").find { File(it).exists() }
    }
}

fun applyNuclearOptionFix() {
    val steamPath = getSteamPath() ?: return
    val userDataDir = File(steamPath, "userdata")
    val steamId = userDataDir.listFiles()?.firstOrNull { it.isDirectory && it.name.all { it.isDigit() } }?.name ?: return
    val configFile = File(userDataDir, "$steamId/config/localconfig.vdf")

    if (!configFile.exists()) return

    val appId = "2168680"
    val newParam = """WINEDLLOVERRIDES="winhttp=n,b" %command%"""
    val lines = configFile.readLines()
    val result = mutableListOf<String>()

    var inTargetApp = false
    var depth = 0
    var optionFound = false

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed == "\"$appId\"") {
            inTargetApp = true
        }

        if (inTargetApp) {
            if (trimmed.contains("{")) depth++
            if (trimmed.contains("}")) depth--

            if (trimmed.startsWith("\"LaunchOptions\"", ignoreCase = true)) {
                continue
            }

            result.add(line)

            if (trimmed == "{" && depth == 1 && !optionFound) {
                result.add("\t\t\t\t\t\"LaunchOptions\"\t\t\"$newParam\"")
                optionFound = true
            }

            if (depth == 0 && trimmed == "}") {
                inTargetApp = false
            }
        } else {
            result.add(line)
        }
    }

    configFile.writeText(result.joinToString("\n"))
}