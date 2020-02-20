package com.dropper.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.dropper.DropperCore

fun main() {
    val config = LwjglApplicationConfiguration()
    config.width = 450
    config.height = 800
    config.title = "Dropper"
    config.samples=5
    LwjglApplication(DropperCore(), config)
}