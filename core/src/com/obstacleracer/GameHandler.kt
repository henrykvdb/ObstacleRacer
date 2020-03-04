package com.obstacleracer

interface GameHandler {
    fun showLeaderboard()
    fun showRateDialog()
    fun showSettingsDialog()
    fun submitScore(score: Int)
    fun getHighscore(): Int
}