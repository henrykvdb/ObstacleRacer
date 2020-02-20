package com.dropper

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import java.util.*

const val DEPTH = 20f
const val RING_SPEED = 5f

class Ring(val color: Color, val rot: Float, var z: Float)

class DropperCore : ApplicationAdapter() {
    private lateinit var cam: Camera
    private lateinit var renderer: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private var start: Long = 0
    private lateinit var textureGate: Texture
    private lateinit var tCircle: Texture

    override fun create() {
        cam = PerspectiveCamera(90f, 0f, 0f)//Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.position.set(0f, 0f, 10f)
        cam.lookAt(0f, 0f, 0f)

        renderer = ShapeRenderer()
        batch = SpriteBatch()

        textureGate = Texture(Gdx.files.internal("gate1.png"))

        updateCamera()
        start = TimeUtils.millis()
    }

    override fun render() {
        val input = cam.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
        input.clamp(0f, 1f)
        cam.position.set(input)
        updateCamera()

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glLineWidth(5f)

        //Lines
        renderer.begin(ShapeRenderer.ShapeType.Line)
        renderer.color = Color.CYAN
        val time = TimeUtils.timeSinceMillis(start)
        for (i in 0 until 8) {
            val vec = Vector3.X.cpy().rotate(Vector3.Z, 360f / 8 * i + time / 40)
            renderer.line(vec + 10f * Vector3.Z, vec - DEPTH * Vector3.Z)
        }
        renderer.end()

        //Rings
        for (ring in rings) {
            batch.transformMatrix.rotate(0f, 0f, 1f, ring.rot)
            batch.transformMatrix.translate(0f, 0f, ring.z)
            batch.begin()
            batch.draw(textureGate, -1f, -1f, 2f, 2f)
            batch.end()
            batch.transformMatrix.translate(0f, 0f, -ring.z)
            batch.transformMatrix.rotate(0f, 0f, 1f, -ring.rot)
        }

        //World update
        if (TimeUtils.nanoTime() - lastSpawnTime > 300000000)
            spawnRing()

        val iter = rings.iterator()
        for (ring in iter) {
            ring.z += Gdx.graphics.deltaTime * RING_SPEED
            if (ring.z >= 10) iter.remove()
        }
    }

    private var rings = ArrayDeque<Ring>()
    private var lastSpawnTime = 0L

    private fun spawnRing() {
        val color = Color(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat(), 1f)
        rings.push(Ring(color, (Math.random() * 360).toFloat(), -DEPTH))

        lastSpawnTime = TimeUtils.nanoTime()
    }

    override fun dispose() {
        renderer.dispose()
        batch.dispose()
        textureGate.dispose()
        tCircle.dispose()
    }

    override fun resize(width: Int, height: Int) {
        cam.viewportWidth = width.toFloat()
        cam.viewportHeight = height.toFloat()
        updateCamera()
    }

    private fun updateCamera() {
        cam.update()

        renderer.projectionMatrix = cam.combined
        batch.projectionMatrix = cam.combined
    }
}