package com.dropper

import com.badlogic.gdx.math.Vector

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

operator fun <T : Vector<T>> Vector<T>.unaryMinus(): T = this.scl(-1f)
