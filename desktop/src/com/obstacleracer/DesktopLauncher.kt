package com.obstacleracer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

var highScore = 0

fun main() {
    val config = Lwjgl3ApplicationConfiguration()
    config.setTitle("Dropper")

    // Enable AA, all default values except samples
    config.setBackBufferConfig(8,8,8,8,16,0, 5)

    Lwjgl3Application(GameAdapter({ Gdx.files.local("assets/") }, object : GameHandler {
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
    }, initialInverted = false), config)
}