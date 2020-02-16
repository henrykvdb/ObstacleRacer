package com.dropper

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.TimeUtils
import kotlin.math.min

class DropperCore : ApplicationAdapter() {
	private lateinit var cam: OrthographicCamera
	private lateinit var renderer: ShapeRenderer
	private lateinit var center: Vector2

	override fun create() {
		cam = OrthographicCamera()
		center = Vector2(Gdx.graphics.width / 2f, Gdx.graphics.height / 2f)
		cam.setToOrtho(true, center.x, center.y)
		renderer = ShapeRenderer()
	}

	override fun render() {
		Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
		Gdx.gl.glLineWidth(5f)

		val frontRadius = min(Gdx.graphics.height, Gdx.graphics.width) / 2
		val touch = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())

		//Clip touch
		val clippedTouch = touch.sub(center)
		if (clippedTouch.len() > frontRadius)
			clippedTouch.scl(frontRadius / clippedTouch.len())
		clippedTouch.add(center)

		//Update camera position
		val percentX = clippedTouch.x / Gdx.graphics.width
		val percentY = clippedTouch.y / Gdx.graphics.height
		cam.position.x = Gdx.graphics.width / 2f + cam.viewportWidth * (percentX - 0.5f)
		cam.position.y = Gdx.graphics.height / 2f + cam.viewportHeight * (percentY - 0.5f)
		cam.update()
		renderer.projectionMatrix = cam.combined

		//Front circle
		renderer.begin(ShapeRenderer.ShapeType.Filled)
		renderer.color = Color.GREEN
		renderer.circle(center.x, center.y, frontRadius.toFloat())
		renderer.end()

		//Position dot
		renderer.begin(ShapeRenderer.ShapeType.Filled)
		renderer.color = Color.BLACK
		renderer.circle(clippedTouch.x, clippedTouch.y, 4f)
		renderer.end()

		//Rings
		if(TimeUtils.nanoTime() - lastSpawnTime > 1000000000) spawnRing()

		for (ring in rings) {
			renderer.begin(ShapeRenderer.ShapeType.Line)
			renderer.color = Color.BLUE
			renderer.circle(Gdx.graphics.width / 2.toFloat(), Gdx.graphics.height / 2.toFloat(), ring)
			renderer.end()
		}

		rings = (rings.map { it + 100f*Gdx.graphics.deltaTime })
				.filter { it<=frontRadius }.toMutableList()
		//TODO filter -> collision check & filter
	}

	private var rings = mutableListOf<Float>()
	private var lastSpawnTime = 0L

	private fun spawnRing() {
		rings.add(20f)
		lastSpawnTime = TimeUtils.nanoTime()
	}

	override fun dispose() {
		renderer.dispose()
	}
}