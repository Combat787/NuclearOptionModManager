package com.combat.nomm

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Nuclear Option Mod Manager"
    ) {
        window.minimumSize = Dimension(800, 600)
        App()
    }
}