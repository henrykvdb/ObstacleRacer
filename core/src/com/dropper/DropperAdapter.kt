package com.dropper

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.files.FileHandle

class DropperAdapter(
        private val fileConstructor: () -> FileHandle,
        private val handler: DropperHandler
) : ApplicationAdapter() {
    private lateinit var core: DropperCore

    override fun create() {
        core = DropperCore(fileConstructor(), handler)
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
}