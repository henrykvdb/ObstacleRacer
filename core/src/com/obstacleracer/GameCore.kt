package com.obstacleracer

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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.TimeUtils
import java.util.Random
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

const val DEPTH = 20f
const val GATE_DISTANCE = 3f
const val GATE_DEPTH = 0.1f

const val GATE_BASE_SPEED = 4f
const val GATE_ACCELERATION = 0.10f
const val GATE_SPEED_EXP = 0.6f

const val GATE_REVERSE_BASE_SPEED = -2f
const val BOUNCE_DISTANCE = GATE_DISTANCE / 2
const val GATE_REVERSE_ACCELERATION = GATE_REVERSE_BASE_SPEED * GATE_REVERSE_BASE_SPEED / BOUNCE_DISTANCE / 2

const val GATE_ROT_START_TIME = 20f
const val GATE_ROT_SPEED = 10f
const val GATE_ROT_SPEED_EXP = 0.5f

const val COLOR_MIN_BRIGHTNESS = 0.4f
const val COLOR_MIN_DISTANCE = 90f

const val LINE_COUNT = 8

const val CAMERA_FRICTION = 0.8f

class Ring(val color: Color, val type: Int, val rotSpeed: Float, var rot: Float, var z: Float)

class DropperCore(files: FileHandle, private val handler: GameHandler, var inverted: Boolean) {
    private val disposables = mutableListOf<Disposable>()

    private val cam = PerspectiveCamera(90f, 0f, 0f).apply {
        position.set(0f, 0f, 0f)
        lookAt(0f, 0f, -1f)
        near = 0.01f
    }

    private val shapeRenderer = ShapeRenderer().disposable()
    private val modelBatch = ModelBatch().disposable()

    private val overlayRenderer = OverlayRenderer(handler).disposable()

    private val models: List<Model>
    private val collisionMeshes: List<CollisionMesh>

    private val joystick = Joystick().disposable()

    private val startTime: Long
    private val random = Random()

    //State
    private val gates = ArrayDeque<Ring>()

    private var time = 0f
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

        models = modelFiles.map { manager.get(it.path()) }
        collisionMeshes = models.map { CollisionMesh(it) }

        val music = manager.get<Music>("music.mp3")
        music.volume = 0.8f
        music.isLooping = true
        music.play()
        music.disposable()

        Gdx.input.inputProcessor = joystick

        updateCamera()
        startTime = TimeUtils.millis()
    }

    private fun touchInput(): Vector2 {
        val minSize = min(Gdx.graphics.width, Gdx.graphics.height).toFloat()
        return Vector2(
                (2f * Gdx.input.x.toFloat() - Gdx.graphics.width) / minSize,
                -(2f * Gdx.input.y.toFloat() - Gdx.graphics.height) / minSize
        ).clamp(0f, 1f)
    }

    fun render() {
        //Update camera based on input
        if (!menu) {
//            val input = touchInput()
            val input = joystick.controlInput()

            //never go exactly to the edge, both rendering and collision would break
            input /= 1.05f

            if (inverted)
                input*=-1f

            cam.position.xy = (1 - CAMERA_FRICTION) * input + CAMERA_FRICTION * cam.position.xy
            updateCamera()
        }

        //Reset background
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClearDepthf(1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        //antialias
        if (Gdx.graphics.bufferFormat.coverageSampling)
            Gdx.gl.glClear(GL20.GL_COVERAGE_BUFFER_BIT_NV)

        //Lines
        Gdx.gl.glLineWidth(5f)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.CYAN

        val colorBits = shapeRenderer.color.toFloatBits()

        for (i in 0 until LINE_COUNT) {
            val angle = 360f / LINE_COUNT * i + time * 25
            val vec1 = Vector2.X.cpy().rotateDeg(angle + 1.5f)
            val vec2 = Vector2.X.cpy().rotateDeg(angle - 1.5f)

            shapeRenderer.renderer.apply {
                color(colorBits)
                vertex(vec2.x, vec2.y, 0f)
                color(colorBits)
                vertex(vec1.x, vec1.y, 0f)
                color(colorBits)
                vertex(vec2.x, vec2.y, -DEPTH)

                color(colorBits)
                vertex(vec1.x, vec1.y, 0f)
                color(colorBits)
                vertex(vec1.x, vec1.y, -DEPTH)
                color(colorBits)
                vertex(vec2.x, vec2.y, -DEPTH)
            }
        }
        shapeRenderer.end()

        //Rings
        modelBatch.begin(cam)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glDepthMask(true)

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

        //if (!menu) joystick.render()

        //World update
        time += Gdx.graphics.deltaTime

        if (gates.isEmpty() || gates.last().z > -DEPTH + GATE_DISTANCE)
            spawnRing()

        val speed = if (menu) {
            min(0f, GATE_REVERSE_BASE_SPEED + 1 / 2f * GATE_REVERSE_ACCELERATION * time.pow(2))
        } else {
            GATE_BASE_SPEED + GATE_ACCELERATION * time.pow(GATE_SPEED_EXP)
        }

        val iter = gates.iterator()
        for (gate in iter) {
            gate.z += Gdx.graphics.deltaTime * speed
            gate.rot += Gdx.graphics.deltaTime * gate.rotSpeed
            if (gate.z > 1.0)
                iter.remove()

            //collision detection
            if (gate.z in -GATE_DEPTH / 2f..GATE_DEPTH / 2f) {
                val meshPosition = cam.position.cpy().rotate(Vector3.Z, -gate.rot).scl(0.5f)
                if (collisionMeshes[gate.type].collides(meshPosition.x, meshPosition.y))
                    die()
            }
        }

        if (menu) {
            val restart = overlayRenderer.renderMenuOverlay(score.toInt(), highscore)
            if (restart) restart()
        } else {
            score += Gdx.graphics.deltaTime * speed
			overlayRenderer.renderGameOverlay(score.toInt())
        }
    }

    private fun die() {
        menu = true
        time = 0f
        gates.forEach { it.z -= GATE_DEPTH * 2 }
        handler.submitScore(score.toInt())
        highscore = handler.getHighscore()
    }

    private fun restart() {
        time = 0f
        score = 0f
        gates.clear()
        menu = false
    }

    private fun spawnRing() {
        val color = Color().apply {
            val prevHue = gates.lastOrNull()?.color?.hue ?: random.nextFloatB(0f, 360f)
            val hue = prevHue + random.nextFloatB(COLOR_MIN_DISTANCE, 360f - COLOR_MIN_DISTANCE)

            val saturation = sqrt(random.nextFloat()) //uniform sampling of hsv cylinder
            val value = random.nextFloatB(COLOR_MIN_BRIGHTNESS, 1f)

            fromHsv(hue, saturation, value)
        }

        val type = random.nextInt(models.size)

        val rotSpeedDeviation = GATE_ROT_SPEED * max(0f, time - GATE_ROT_START_TIME).pow(GATE_ROT_SPEED_EXP)
        val rotSpeed = random.nextGaussian().toFloat() * rotSpeedDeviation
        val rot = random.nextFloatB(0f, 360f)
        gates.add(Ring(color, type, rotSpeed, rot, -DEPTH))
    }

    fun resize(width: Int, height: Int) {
        overlayRenderer.resize(width, height)

        cam.viewportWidth = width.toFloat()
        cam.viewportHeight = height.toFloat()
        updateCamera()

        joystick.resize(width, height)
    }

    private fun updateCamera() {
        cam.update()

        shapeRenderer.projectionMatrix = cam.combined
    }

    private fun <T : Disposable> T.disposable() = this.also { disposables += it }

    fun dispose() {
        disposables.asReversed().forEach(Disposable::dispose)
    }
}