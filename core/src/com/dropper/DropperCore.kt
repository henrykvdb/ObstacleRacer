package com.dropper

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.files.FileHandle
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
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.TimeUtils
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

const val DEPTH = 20f
const val GATE_DISTANCE = 3f
const val GATE_DEPTH = 0.1f

const val GATE_BASE_SPEED = 4f
const val GATE_ACCELERATION = 0.05f

const val GATE_REVERSE_BASE_SPEED = -2f
const val BOUNCE_DISTANCE = GATE_DISTANCE/2
const val GATE_REVERSE_ACCELERATION = GATE_REVERSE_BASE_SPEED*GATE_REVERSE_BASE_SPEED/ BOUNCE_DISTANCE/2

class Ring(val color: Color, val type: Int, val rot: Float, var z: Float)

class DropperCore(files: FileHandle, private val handler: DropperHandler) {
    private val disposables = mutableListOf<Disposable>()

    private val cam = PerspectiveCamera(90f, 0f, 0f).apply {
        position.set(0f, 0f, 0f)
        lookAt(0f, 0f, -1f)
        near = 0f
    }

    private val renderer = ShapeRenderer().disposable()
    private val modelBatch = ModelBatch { camera, renderables ->
        val default = DefaultRenderableSorter()
        default.sort(camera, renderables)
        renderables.reverse()
    }.disposable()

    private val textRenderer = TextRenderer((min(Gdx.graphics.width, Gdx.graphics.height) * 0.1).roundToInt()).disposable()
    private val fpsRenderer = TextRenderer((min(Gdx.graphics.width, Gdx.graphics.height) * 0.03).roundToInt()).disposable()
    private var menuRenderer = MenuRenderer(handler).disposable()

    private val models: List<Model>
    private val collisionMeshes: List<CollisionMesh>

    private val startTime: Long
    private val random = Random()

    //State
    private var gates = ArrayDeque<Ring>()
    private var speed = 0f
    private var score = 0f
    private var highscore = handler.getHighscore()
    private var menu = true

    init {
        //asset loading
        val manager = AssetManager().disposable()
        manager.load("music.mp3", Music::class.java)
        val modelFiles = files.child("gates").list(".g3db")
        for (file in modelFiles) {
            manager.load(file.path(), Model::class.java)
        }

        manager.finishLoading()

        models = modelFiles.map { manager.get<Model>(it.path()) }
        collisionMeshes = models.map { CollisionMesh(it) }

        val music = manager.get<Music>("music.mp3")
        music.volume = 0.8f
        music.isLooping = true
        music.play()
        music.disposable()

        updateCamera()
        startTime = TimeUtils.millis()
    }

    fun render() {
        //Update camera
        if (!menu){
            val minSize = min(Gdx.graphics.width, Gdx.graphics.height).toFloat()
            val input = Vector2(
                    (2f * Gdx.input.x.toFloat() - Gdx.graphics.width) / minSize,
                    -(2f * Gdx.input.y.toFloat() - Gdx.graphics.height) / minSize
            ).clamp(0f, 1f)

            //never go exactly to the edge, both rendering and collision would break
            cam.position.set(input.x / 1.05f, input.y / 1.05f, 0f)
            updateCamera()
        }

        //Reset background
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        //antialias
        if (Gdx.graphics.bufferFormat.coverageSampling)
            Gdx.gl.glClear(GL20.GL_COVERAGE_BUFFER_BIT_NV)

        //Lines
        Gdx.gl.glLineWidth(5f)
        val elapsedTime = TimeUtils.timeSinceMillis(startTime)
        renderer.begin(ShapeRenderer.ShapeType.Line)
        renderer.color = Color.CYAN
        for (i in 0 until 8) {
            val vec = Vector3.X.cpy().rotate(Vector3.Z, 360f / 8 * i + elapsedTime / 40)
            renderer.line(vec + Vector3.Z, vec - DEPTH * Vector3.Z)
        }
        renderer.end()

        //Rings
        modelBatch.begin(cam)
        modelBatch.render(gates.map { ring ->
            val transform = Matrix4()
                    .translate(0f, 0f, ring.z)
                    .rotate(Vector3.Z, ring.rot)
                    .rotate(Vector3.X, -90f)
                    .scale(2f, 2f, 2f)

            ModelInstance(models[ring.type], transform).apply {
                materials[0].set(ColorAttribute(ColorAttribute.Diffuse, ring.color))
                materials[1].set(ColorAttribute(ColorAttribute.Diffuse, Color.WHITE))
            }
        })
        modelBatch.end()

        //fps
        val fps = Gdx.graphics.framesPerSecond
        fpsRenderer.renderc("$fps fps", Gdx.graphics.width / 2f, Gdx.graphics.height - textRenderer.height / 2f)

        //World update
        if (gates.isEmpty() || gates.first.z > -DEPTH + GATE_DISTANCE)
            spawnRing()

        val iter = gates.iterator()
        for (gate in iter) {
            gate.z += Gdx.graphics.deltaTime * speed
            if (gate.z > 1.0)
                iter.remove()

            //collision detection
            if (gate.z in -GATE_DEPTH / 2f..GATE_DEPTH / 2f) {
                val meshPosition = cam.position.cpy().rotate(Vector3.Z, -gate.rot).scl(0.5f)
                if (collisionMeshes[gate.type].collides(meshPosition.x, meshPosition.y))
                    die()
            }
        }

        if (menu){
            speed += Gdx.graphics.deltaTime * GATE_REVERSE_ACCELERATION
            speed = min(speed,0f)

            val restart = menuRenderer.renderScore(score.toInt(),highscore)
            if (restart) restart()
        }
        else {
            score += Gdx.graphics.deltaTime * speed
            speed += Gdx.graphics.deltaTime * GATE_ACCELERATION
            textRenderer.renderc("${score.toInt()}", Gdx.graphics.width / 2f, textRenderer.height / 2f)
        }
    }

    private fun die() {
        menu = true
        speed = GATE_REVERSE_BASE_SPEED
        gates.forEach { it.z -= GATE_DEPTH*2 }
        handler.submitScore(score.toInt())
        highscore = handler.getHighscore()
    }

    private fun restart() {
        score = 0f
        speed = GATE_BASE_SPEED
        gates.clear()
        menu = false
    }

    private fun spawnRing() {
        val color = Color(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat(), 1f)
        val type = random.nextInt(models.size)
        val rot = (Math.random() * 360).toFloat()
        gates.push(Ring(color, type, rot, -DEPTH))
    }

    fun resize(width: Int, height: Int) {
        textRenderer.resize(width, height)
        fpsRenderer.resize(width, height)
        menuRenderer.resize(width, height)

        cam.viewportWidth = width.toFloat()
        cam.viewportHeight = height.toFloat()
        updateCamera()
    }

    private fun updateCamera() {
        cam.update()

        renderer.projectionMatrix = cam.combined
    }

    private fun <T : Disposable> T.disposable() = this.also { disposables += it }

    fun dispose() {
        disposables.asReversed().forEach(Disposable::dispose)
    }
}