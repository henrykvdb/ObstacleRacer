package com.dropper

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.files.FileHandle
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
import kotlin.math.roundToInt

const val DEPTH = 20f
const val GATE_DISTANCE = 3f
const val GATE_BASE_SPEED = 4f
const val GATE_ACCELERATION = 0.05f

class Ring(val color: Color, val type: Int, val rot: Float, var z: Float)

class DropperCore(private val files: () -> FileHandle, private val handler: DropperHandler) : ApplicationAdapter() {
    private lateinit var manager: AssetManager
    private lateinit var music: Music
    private lateinit var textRenderer: TextRenderer
    private lateinit var fpsRenderer: TextRenderer

    private lateinit var cam: Camera
    private lateinit var renderer: ShapeRenderer
    private lateinit var modelBatch: ModelBatch
    private lateinit var models: List<Model>

    private var startTime: Long = 0
    private val random = Random()

    private var speed = GATE_BASE_SPEED
    private var score = 0f

    override fun create() {
        textRenderer = TextRenderer((min(Gdx.graphics.width, Gdx.graphics.height) * 0.1).roundToInt())
        fpsRenderer = TextRenderer((min(Gdx.graphics.width, Gdx.graphics.height) * 0.03).roundToInt())

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
        val modelFiles = files().child("gates").list(".g3db")
        for (file in modelFiles) {
            manager.load(file.path(), Model::class.java)
        }
        manager.finishLoading()
        models = modelFiles.map { manager.get<Model>(it.path()) }

        updateCamera()
        startTime = TimeUtils.millis()

        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"))
        music.volume = 0.8f
        music.isLooping = true
        music.play()
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
        val elapsedTime = TimeUtils.timeSinceMillis(startTime)
        renderer.begin(ShapeRenderer.ShapeType.Line)
        renderer.color = Color.CYAN
        for (i in 0 until 8) {
            val vec = Vector3.X.cpy().rotate(Vector3.Z, 360f / 8 * i + elapsedTime / 40)
            renderer.line(vec + Vector3.Z, vec - DEPTH * Vector3.Z)
        }
        renderer.end()

        //Rings
        val instances = gates.map { ring ->
            val transform = Matrix4()
                    .translate(0f, 0f, ring.z)
                    .rotate(Vector3.Z, ring.rot)
                    .rotate(Vector3.X, 90f)
                    .scale(2f, 2f, 2f)

            ModelInstance(models[ring.type], transform).apply {
                materials[0].set(ColorAttribute(ColorAttribute.Diffuse, ring.color))
                materials[1].set(ColorAttribute(ColorAttribute.Diffuse, Color.WHITE))
            }
        }

        modelBatch.begin(cam)
        modelBatch.render(instances)
        modelBatch.end()

        //fps
        val fps = Gdx.graphics.framesPerSecond
        fpsRenderer.render("$fps fps", Gdx.graphics.width / 2f, Gdx.graphics.height - textRenderer.size / 2f)
        textRenderer.render("${score.toInt()}", Gdx.graphics.width / 2f, textRenderer.size / 2f)

        //World update
        if (gates.isEmpty() || gates.first.z > -DEPTH + GATE_DISTANCE)
            spawnRing()

        val iter = gates.iterator()
        for (gate in iter) {
            gate.z += Gdx.graphics.deltaTime * speed
            if (gate.z > 1.0)
                iter.remove()
        }

        speed += Gdx.graphics.deltaTime * GATE_ACCELERATION
        score += Gdx.graphics.deltaTime * speed
    }

    private var gates = ArrayDeque<Ring>()
    private var lastSpawnTime = 0L

    private fun spawnRing() {
        val color = Color(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat(), 1f)
        val type = random.nextInt(models.size)
        gates.push(Ring(color, type, (Math.random() * 360).toFloat(), -DEPTH))

        lastSpawnTime = TimeUtils.nanoTime()
    }

    override fun dispose() {
        textRenderer.dispose()
        fpsRenderer.dispose()
        renderer.dispose()
        modelBatch.dispose()
        manager.dispose()
        music.dispose()
    }

    override fun resize(width: Int, height: Int) {
        textRenderer.resize(width, height)
        fpsRenderer.resize(width, height)

        cam.viewportWidth = width.toFloat()
        cam.viewportHeight = height.toFloat()
        updateCamera()
    }

    private fun updateCamera() {
        cam.update()

        renderer.projectionMatrix = cam.combined
    }
}