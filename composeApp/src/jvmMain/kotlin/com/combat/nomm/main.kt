package com.combat.nomm

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import nuclearoptionmodmanager.composeapp.generated.resources.Res
import nuclearoptionmodmanager.composeapp.generated.resources.iconpng
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension

val LocalWindowState = compositionLocalOf<WindowState> {
    error("No WindowState provided")
}

fun main() = application {
    val windowState = rememberWindowState()
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Nuclear Option Mod Manager",
        state = windowState,
        icon = painterResource(Res.drawable.iconpng)
    ) {
        CompositionLocalProvider(LocalWindowState provides windowState) {
            val currentConfig by SettingsManager.config
            window.minimumSize = Dimension(640, 480)
            NommTheme(
                currentConfig.themeColor, when (currentConfig.theme) {
                    Theme.DARK -> true
                    Theme.LIGHT -> false
                    Theme.SYSTEM -> isSystemInDarkTheme()
                }, currentConfig.paletteStyle,
                currentConfig.contrast
            ) {
                App()
            }
        }
    }
}