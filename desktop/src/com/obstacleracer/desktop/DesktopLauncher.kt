package com.obstacleracer.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.obstacleracer.GameAdapter
import com.obstacleracer.GameHandler

var highScore = 0

fun main() {
    val config = LwjglApplicationConfiguration()
    config.width = 450
    config.height = 800
    config.title = "Dropper"
    config.samples = 5

    LwjglApplication(GameAdapter({ Gdx.files.local("") }, object : GameHandler {
        override fun showLeaderboard() {
            println("Request: show leaderboard")
        }

        override fun showRateDialog() {
            println("Request: show rate dialog")
        }

        override fun showSettingsDialog() {
            println("Request: show about dialog")
        }

        override fun submitScore(score: Int) {
            println("Request: submit score $score")
            if (score > highScore)
                highScore = score
        }

        override fun getHighscore(): Int {
            println("Request: get highscore")
            return highScore
        }
    }), config)
}