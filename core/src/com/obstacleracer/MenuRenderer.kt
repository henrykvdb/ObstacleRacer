package com.obstacleracer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.utils.Disposable
import kotlin.math.min
import kotlin.math.roundToInt


class MenuRenderer(private val handler: GameHandler) : Disposable {
    val minDim = min(Gdx.graphics.width, Gdx.graphics.height)

    private var shapeRenderer: ShapeRenderer = ShapeRenderer()
    private var batch: SpriteBatch = SpriteBatch()
    private val textRenderer = TextRenderer((minDim * 0.1).roundToInt())
    private val subtextRenderer = TextRenderer((minDim * 0.05).roundToInt())
    private var cam = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    private val btnLeaderboard = Texture(Gdx.files.internal("buttons/leaderboard.png"))
    private val btnAbout = Texture(Gdx.files.internal("buttons/about.png"))
    private val btnRate = Texture(Gdx.files.internal("buttons/rate.png"))

    fun resize(screenWidth: Int, screenHeight: Int) {
        textRenderer.resize(screenWidth, screenHeight)
        subtextRenderer.resize(screenWidth, screenHeight)
        cam = OrthographicCamera(screenWidth.toFloat(), screenHeight.toFloat())
        cam.translate(screenWidth / 2.toFloat(), screenHeight / 2.toFloat())
        cam.update()
        shapeRenderer.projectionMatrix = cam.combined
        batch.projectionMatrix = cam.combined
    }

    fun renderScore(score: Int, highscore: Int): Boolean {
        //Rectangle
        val minDim = min(Gdx.graphics.width, Gdx.graphics.height)
        val wSize = minDim * 0.8f
        val hSize = if(score == 0) 2f*textRenderer.height else 3f * textRenderer.height
        val wStart = (Gdx.graphics.width - wSize) / 2f
        val hStart = (Gdx.graphics.height - hSize) / 2f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.5f, 0.5f, 0.5f, 0.8f)
        shapeRenderer.rect(0f, hStart, Gdx.graphics.width.toFloat(), hSize)
        shapeRenderer.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)
        val restartMenu = score != 0

        //Score text
        var height = hStart + (textRenderer.height - subtextRenderer.height) / 2f
        if (restartMenu) {
            textRenderer.render("SCORE", wStart, height)
            textRenderer.render(score.toString(), wStart + wSize - textRenderer.width(score.toString()), height)
            height += textRenderer.height
        }

        //Best text
        textRenderer.render(if (restartMenu) "BEST" else "HIGHSCORE", wStart, height)
        textRenderer.render(highscore.toString(), wStart + wSize - textRenderer.width(highscore.toString()), height)
        height += textRenderer.height

        //var height = hStart
        subtextRenderer.render("Touch to ${if (restartMenu) "restart" else "start"}", wStart, height)

        //Buttons
        val edgePad = minDim*0.04f
        val betweenPad = minDim*0.01f
        val size = (wSize - 2 * (betweenPad + edgePad)) / 3
        height = hStart + hSize + betweenPad
        batch.begin()
        batch.draw(btnAbout, wStart + edgePad, height, size, size)
        batch.draw(btnRate, (Gdx.graphics.width - size) / 2, height, size, size)
        batch.draw(btnLeaderboard, wSize + wStart - size - edgePad, height, size, size)
        batch.end()

        //Handle input
        if (Gdx.input.justTouched()) {
            val r = size / 2
            val about = Circle(wStart + edgePad + r, height + r, r)
            val rate = Circle((Gdx.graphics.width - size) / 2 + r, height + r, r)
            val leaderboard = Circle(wSize + wStart - size - edgePad + r, height + r, r)

            val inX = Gdx.input.x.toFloat()
            val inY = Gdx.graphics.height - Gdx.input.y.toFloat()

            when {
                about.contains(inX, inY) -> handler.showAboutDialog()
                rate.contains(inX, inY) -> handler.showRateDialog()
                leaderboard.contains(inX, inY) -> handler.showLeaderboard()
                else -> return true
            }
        }

        return false
    }

    override fun dispose() {
        batch.dispose()
        textRenderer.dispose()
        subtextRenderer.dispose()
        shapeRenderer.dispose()
        btnAbout.dispose()
        btnLeaderboard.dispose()
        btnRate.dispose()
    }
}