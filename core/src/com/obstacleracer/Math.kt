package com.obstacleracer

import com.badlogic.gdx.math.Vector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

operator fun <T : Vector<T>> Vector<T>.plus(other: T): T = this.cpy().add(other)
operator fun <T : Vector<T>> Vector<T>.plusAssign(other: T) {
    this.add(other)
}

operator fun <T : Vector<T>> Vector<T>.minus(other: T): T = this.cpy().add(-other)
operator fun <T : Vector<T>> Vector<T>.minusAssign(other: T) {
    this.add(-other)
}

operator fun <T : Vector<T>> Vector<T>.times(scalar: Float): T = this.cpy().scl(scalar)
operator fun <T : Vector<T>> Float.times(other: Vector<T>): T = other.cpy().scl(this)
operator fun <T : Vector<T>> Vector<T>.timesAssign(scalar: Float) {
    this.scl(scalar)
}

operator fun <T : Vector<T>> Vector<T>.div(scalar: Float): T = this.cpy().scl(1 / scalar)
operator fun <T : Vector<T>> Vector<T>.divAssign(scalar: Float) {
    this.scl(1 / scalar)
}

operator fun <T : Vector<T>> Vector<T>.unaryMinus(): T = this.cpy().scl(-1f)

val Float.Companion.SIZE_BYTES get() = 4

var Vector3.xy: Vector2
    get() = Vector2(this.x, this.y)
    set(value) {
        this.set(value, this.z)
    }