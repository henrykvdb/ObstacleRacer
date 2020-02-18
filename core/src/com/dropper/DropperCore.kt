package com.dropper

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import java.util.*

const val DEPTH = 20f
const val RING_SPEED = 5f

class Ring(val color: Color, var z: Float)

class DropperCore : ApplicationAdapter() {
    private lateinit var cam: Camera
    private lateinit var renderer: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private var start: Long = 0

    override fun create() {
        cam = PerspectiveCamera(90f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.position.set(0f, 0f, 10f)
        cam.lookAt(0f, 0f, 0f)
        cam.update()

        renderer = ShapeRenderer()
        batch = SpriteBatch()

        start = TimeUtils.millis()
    }

    override fun render() {
        val input = cam.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
        input.clamp(0f, 1f)
        cam.position.set(input)
        cam.update()

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glLineWidth(5f)

        renderer.projectionMatrix = cam.combined

        //Front circle
        /*renderer.begin(ShapeRenderer.ShapeType.Filled)
        renderer.color = Color.GREEN
        renderer.circle(0f, 0f, 1f)
        renderer.end()*/

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
        if (TimeUtils.nanoTime() - lastSpawnTime > 300000000) spawnRing()

        renderer.begin(ShapeRenderer.ShapeType.Line)
        for (ring in rings) {
            renderer.translate(0f, 0f, ring.z)
            renderer.color = ring.color
            renderer.circle(0f, 0f, 1f, 40)
            renderer.translate(0f, 0f, -ring.z)
        }
        renderer.end()

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
        rings.push(Ring(color, -DEPTH))

        lastSpawnTime = TimeUtils.nanoTime()
    }

    override fun dispose() {
        renderer.dispose()
    }

    override fun resize(width: Int, height: Int) {
        cam.viewportWidth = width.toFloat()
        cam.viewportHeight = height.toFloat()
        cam.update()
    }
}