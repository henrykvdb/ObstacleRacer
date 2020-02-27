package com.obstacleracer

interface GameHandler {
    fun showLeaderboard()
    fun showRateDialog()
    fun showAboutDialog()
    fun submitScore(score: Int)
    fun getHighscore(): Int
}