package com.dropper

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Disposable


class TextRenderer(val height: Int) : Disposable {
    private var batch: SpriteBatch = SpriteBatch()
    private var cam: OrthographicCamera
    private var font: BitmapFont

    init {
        cam = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        val generator = FreeTypeFontGenerator(Gdx.files.internal("font.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = height
        font = generator.generateFont(parameter) // font size 12 pixels
        generator.dispose() // don't forget to dispose to avoid memory leaks!
    }

    fun resize(screenWidth: Int, screenHeight: Int) {
        cam = OrthographicCamera(screenWidth.toFloat(), screenHeight.toFloat())
        cam.translate(screenWidth / 2.toFloat(), screenHeight / 2.toFloat())
        cam.update()
        batch.projectionMatrix = cam.combined
    }

    //Render centered at (posX, posY)
    fun renderc(text: String, posX: Float, posY: Float) {
        val layout = GlyphLayout(font, text)
        batch.begin()
        font.draw(batch, layout, posX - layout.width / 2f, Gdx.graphics.height - posY + layout.height / 2f)
        batch.end()
    }

    fun width(text: String) = GlyphLayout(font, text).width

    //Render at (posX, posY)
    fun render(text: String, posX: Float, posY: Float) {
        batch.begin()
        font.draw(batch, GlyphLayout(font, text), posX, Gdx.graphics.height - posY)
        batch.end()
    }

    override fun dispose() {
        font.dispose()
        batch.dispose()
    }
}