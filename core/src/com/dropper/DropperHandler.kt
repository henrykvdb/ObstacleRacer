package com.dropper

interface DropperHandler {
    fun showLeaderboard()
    fun showRateDialog()
    fun showAboutDialog()
    fun submitScore(score: Int)
    fun getHighscore(): Int
}