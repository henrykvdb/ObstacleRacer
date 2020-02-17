package com.dropper

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.TimeUtils

class DropperCore : ApplicationAdapter() {
    private lateinit var cam: OrthographicCamera
    private lateinit var renderer: ShapeRenderer
    private var aspectRatio = 0f

    override fun create() {
        aspectRatio = Gdx.graphics.height.toFloat() / Gdx.graphics.width
        cam = OrthographicCamera()
        cam.setToOrtho(false, 2f, 2f * aspectRatio)
        cam.zoom = 0.5f // don't touch, big dumb
        cam.position.x = 0f
        cam.position.y = 0f
        cam.update()
        renderer = ShapeRenderer()
    }

    override fun render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glLineWidth(5f)

        //Update camera
        val inX = -1f + 2 * Gdx.input.x.toFloat() / Gdx.graphics.width
        val inY = (1f - 2 * Gdx.input.y.toFloat() / Gdx.graphics.height) * aspectRatio
        val input = Vector2(inX,inY).clamp(0f,1f)
        cam.position.x = input.x*cam.zoom
        cam.position.y = input.y*cam.zoom
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