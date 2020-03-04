package com.obstacleracer

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.files.FileHandle

class GameAdapter(
        private val fileConstructor: () -> FileHandle,
        private val handler: GameHandler,
        private val initialInverted: Boolean
) : ApplicationAdapter() {
    private lateinit var core: DropperCore

    override fun create() {
        core = DropperCore(fileConstructor(), handler, initialInverted)
    }

    override fun render() {
        core.render()
    }

    override fun pause() {}

    override fun resume() {}

    override fun resize(width: Int, height: Int) {
        core.resize(width, height)
    }

    override fun dispose() {
        core.dispose()
    }

    fun setInverted(inverted: Boolean){
        core.inverted = inverted
    }
}