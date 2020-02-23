package com.dropper.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.dropper.DropperCore
import com.dropper.DropperHandler

fun main() {
    val config = LwjglApplicationConfiguration()
    config.width = 450
    config.height = 800
    config.title = "Dropper"
    config.samples = 5

    LwjglApplication(DropperCore({ Gdx.files.local("") }, object : DropperHandler {
        override fun showAd() {
            println("Request: ad")
        }

        override fun showLeaderboard() {
            println("Request: show leaderboard")
        }

        override fun submitLeaderboard(score: Int) {
            println("Request: submit leaderboard")
        }
    }), config)
}