package com.dropper

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.DefaultRenderableSorter
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import java.util.*
import kotlin.math.min

const val DEPTH = 20f
const val RING_SPEED = 5f

class Ring(val color: Color, val rot: Float, var z: Float)

class DropperCore : ApplicationAdapter() {
    private lateinit var frameRate: FrameRate

    private lateinit var cam: Camera

    private lateinit var manager: AssetManager

    private lateinit var renderer: ShapeRenderer
    private lateinit var modelBatch: ModelBatch

    private lateinit var modelGate: Model

    private var start: Long = 0

    override fun create() {
        frameRate = FrameRate()

        cam = PerspectiveCamera(90f, 0f, 0f)
        cam.position.set(0f, 0f, 0f)
        cam.lookAt(0f, 0f, 0f)
        cam.near = 0f

        renderer = ShapeRenderer()
        modelBatch = ModelBatch { camera, renderables ->
            val default = DefaultRenderableSorter()
            default.sort(camera, renderables)
            renderables.reverse()
        }

        manager = AssetManager()

        val modelGatePath = "gate1.g3db"
        manager.load(modelGatePath, Model::class.java)

        manager.finishLoading()
        modelGate = manager.get(modelGatePath)

        updateCamera()
        start = TimeUtils.millis()
    }

    override fun render() {
        val minSize = min(Gdx.graphics.width, Gdx.graphics.height).toFloat()
        val input = Vector2(
                (2f * Gdx.input.x.toFloat() - Gdx.graphics.width) / minSize,
                -(2f * Gdx.input.y.toFloat() - Gdx.graphics.height) / minSize
        )
        input.clamp(0f, 1f)
        cam.position.set(input.x, input.y, 0f)
        updateCamera()

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        //antialias
        if (Gdx.graphics.bufferFormat.coverageSampling)
            Gdx.gl.glClear(GL20.GL_COVERAGE_BUFFER_BIT_NV)

        Gdx.gl.glLineWidth(5f)

        //Lines
        renderer.begin(ShapeRenderer.ShapeType.Line)
        renderer.color = Color.CYAN
        val time = TimeUtils.timeSinceMillis(start)
        for (i in 0 until 8) {
            val vec = Vector3.X.cpy().rotate(Vector3.Z, 360f / 8 * i + time / 40)
            renderer.line(vec + Vector3.Z, vec - DEPTH * Vector3.Z)
        }
        renderer.end()

        //Rings
        val instances = rings.map { ring ->
            val transform = Matrix4()
                    .translate(0f, 0f, ring.z)
                    .rotate(Vector3.Z, ring.rot)
                    .rotate(Vector3.X, 90f)
                    .scale(2f, 2f, 2f)

            val instance = ModelInstance(
                    modelGate,
                    transform
            )

            val mat = instance.model.materials.single()
            mat.set(ColorAttribute(ColorAttribute.Diffuse, ring.color))

            instance
        }

        modelBatch.begin(cam)
        modelBatch.render(instances)
        modelBatch.end()

        //fps
        frameRate.update()
        frameRate.render()

        //World update
        if (TimeUtils.nanoTime() - lastSpawnTime > 300000000)
            spawnRing()

        val iter = rings.iterator()
        for (ring in iter) {
            ring.z += Gdx.graphics.deltaTime * RING_SPEED
            if (ring.z > 1.0) iter.remove()
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
        frameRate.dispose()
        renderer.dispose()
        modelBatch.dispose()
        manager.dispose()
    }

    override fun resize(width: Int, height: Int) {
        frameRate.resize(width, height)

        cam.viewportWidth = width.toFloat()
        cam.viewportHeight = height.toFloat()
        updateCamera()
    }

    private fun updateCamera() {
        cam.update()

        renderer.projectionMatrix = cam.combined
    }
}