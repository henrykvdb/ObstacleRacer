package com.obstacleracer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import kotlin.math.min
import kotlin.math.pow

//as a fraction of the minimum screen size
const val JOYSTICK_SIZE = 1 / 6f

class Joystick(val hold: Boolean) : InputAdapter(), Disposable {
    private val camera = OrthographicCamera(0f, 0f)
    private val shapeRenderer = ShapeRenderer()

    private var size: Float = 0f

    private var center: Vector2? = null //in pixels
    private var handle: Vector2 = Vector2(0f, 0f) //relative to size, clamped to unit vector

    init {
        resize(Gdx.graphics.width, Gdx.graphics.height)
    }

    fun render() {
        //enable transparency rendering
        Gdx.graphics.gL20.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.setColor(0.9f, 0.9f, 0.9f, 0.3f)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        center?.let { center ->
            shapeRenderer.circle(center.x, center.y, size, 20)

            val handlePos = center + handle * size
            shapeRenderer.circle(handlePos.x, handlePos.y, size / 4)
        }

        shapeRenderer.end()

        Gdx.graphics.gL20.glDisable(GL20.GL_BLEND)
    }

    fun controlInput(): Vector2 {
        val len = handle.len()
        val newLen = 1 / 2f * (len + len.pow(1.5f))

        return handle.cpy().setLength(newLen)
    }

    fun resize(width: Int, height: Int) {
        size = JOYSTICK_SIZE * min(Gdx.graphics.width, Gdx.graphics.height).toFloat()

        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.update()

        shapeRenderer.projectionMatrix.set(camera.combined)
        shapeRenderer.updateMatrices()
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer != 0)
            return false

        center = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f)).xy -
                 if (hold) handle * size else Vector2.Zero
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (center == null || pointer != 0)
            return false

        center = null
        if (!hold)
            handle = Vector2(0f, 0f)
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val center = center
        if (center == null || pointer != 0)
            return false

        val coords = Vector3(screenX.toFloat(), screenY.toFloat(), 0f)
        camera.unproject(coords)

        val handleBeforeClamp = (coords.xy - center) / size
        handle = handleBeforeClamp.cpy().clamp(0f, 1f)

        center += (handleBeforeClamp - handle) * size
        return true
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}