package com.dropper

interface DropperHandler {
    fun showAd()
    fun showLeaderboard()
    fun submitLeaderboard(score: Int)
}