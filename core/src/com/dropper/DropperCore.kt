package com.dropper

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import kotlin.math.min


class DropperCore : ApplicationAdapter() {
    private lateinit var cam: OrthographicCamera
    private lateinit var renderer: ShapeRenderer
    private lateinit var batch: SpriteBatch

    override fun create() {
        cam = OrthographicCamera()
        cam.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.zoom = 1f / min(Gdx.graphics.width, Gdx.graphics.height)
        cam.position.x = 0f
        cam.position.y = 0f
        cam.update()
        renderer = ShapeRenderer()
        batch = SpriteBatch()
    }

    override fun render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glLineWidth(5f)

        //Update camera
        val input = cam.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
        input.clamp(0f, 1f)
        cam.position.x = input.x / 2
        cam.position.y = input.y / 2
        cam.update()
        renderer.projectionMatrix = cam.combined

        //Front circle
        renderer.begin(ShapeRenderer.ShapeType.Filled)
        renderer.color = Color.GREEN
        renderer.circle(0f, 0f, 1f)
        renderer.end()

        //Position dot
        renderer.begin(ShapeRenderer.ShapeType.Filled)
        renderer.color = Color.RED
        renderer.circle(input.x, input.y, 0.2f)
        renderer.end()

        //Rings
        if (TimeUtils.nanoTime() - lastSpawnTime > 1500000000) spawnRing()

        for (ring in rings) {
            renderer.begin(ShapeRenderer.ShapeType.Line)
            renderer.color = Color.BLUE
            renderer.circle(0f, 0f, ring)
            renderer.end()
        }

        rings = (rings.map { it + Gdx.graphics.deltaTime })
                .filter { it <= 1 }.toMutableList()
        //TODO filter -> collision check & filter

    }

    private var rings = mutableListOf<Float>()
    private var lastSpawnTime = 0L

    private fun spawnRing() {
        rings.add(0.1f)
        lastSpawnTime = TimeUtils.nanoTime()
    }

    override fun dispose() {
        renderer.dispose()
    }
}