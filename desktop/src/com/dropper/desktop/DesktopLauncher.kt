package com.dropper.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.dropper.DropperAdapter
import com.dropper.DropperHandler

var highScore = 0

fun main() {
    val config = LwjglApplicationConfiguration()
    config.width = 450
    config.height = 800
    config.title = "Dropper"
    config.samples = 5

    LwjglApplication(DropperAdapter({ Gdx.files.local("") }, object : DropperHandler {
        override fun showLeaderboard() {
            println("Request: show leaderboard")
        }

        override fun showRateDialog() {
            println("Request: show rate dialog")
        }

        override fun showAboutDialog() {
            println("Request: show about dialog")
        }

        override fun submitScore(score: Int) {
            println("Request: submit score $score")
            if (score>highScore)
                highScore = score
        }

        override fun getHighscore(): Int {
            println("Request: get highscore")
            return highScore
        }
    }), config)
}