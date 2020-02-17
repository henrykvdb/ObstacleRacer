package com.dropper

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import kotlin.math.min


class DropperCore : ApplicationAdapter() {
    private lateinit var cam: OrthographicCamera
    private lateinit var renderer: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private lateinit var tGate: Texture
    private lateinit var tCircle: Texture

    override fun create() {
        cam = OrthographicCamera()
        cam.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.zoom = 1f / min(Gdx.graphics.width, Gdx.graphics.height)
        cam.position.x = 0f
        cam.position.y = 0f
        cam.update()
        renderer = ShapeRenderer()
        batch = SpriteBatch()
        tGate = Texture(Gdx.files.internal("gate1.png"))
        tCircle = Texture(Gdx.files.internal("circle.png"))
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
        batch.projectionMatrix = cam.combined

        //Back circle
        batch.begin()
        batch.color = Color.WHITE
        batch.draw(tCircle, -1f, -1f, 2f, 2f)
        batch.end()

        //Rings
        if (TimeUtils.nanoTime() - lastSpawnTime > 1000000000) spawnRing()

        for (ring in rings.reversed()) {
            batch.begin()
            batch.draw(tGate, -ring/2, -ring/2, ring,ring)
            batch.end()
        }

        //Position dot
        batch.begin()
        val size = 0.1f
        batch.color = Color.RED
        batch.draw(tCircle, input.x-size/2, input.y-size/2, size, size)
        batch.end()

        rings = (rings.map { it + 1*Gdx.graphics.deltaTime })
                .filter { it <= 2 }.toMutableList()
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