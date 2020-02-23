package com.dropper

import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.math.Intersector

class CollisionMesh(model: Model) {
    //(ax, ay, bx, by, cx, cy) for each triangle
    private val data: FloatArray
    private val triangleCount: Int

    init {
        val mesh = model.meshes.single()

        val attr = mesh.getVertexAttribute(VertexAttributes.Usage.Position)
        check(attr.numComponents == 3)

        val meshTriangleCount = mesh.numIndices / 3

        var triangleCount = 0
        val data = FloatArray(meshTriangleCount * 6)

        outer@ for (t in 0 until meshTriangleCount) {
            val indices = ShortArray(3)
            mesh.getIndices(3 * t, 3, indices, 0)

            for (c in 0 until 3) {
                val index = indices[c]
                val position = FloatArray(3)
                mesh.getVertices(mesh.vertexSize / Float.SIZE_BYTES * index + attr.offset, 3, position, 0)

                //we only need the triangles of one of the faces, so skip the other face and the sides
                if (position[1] > 0) continue@outer

                data[6 * triangleCount + 2 * c] = position[0] //x
                data[6 * triangleCount + 2 * c + 1] = position[2] //z -> y
            }

            triangleCount++
        }

        this.triangleCount = triangleCount
        this.data = data.copyOf(6 * triangleCount)
    }

    fun collides(px: Float, py: Float) = (0 until triangleCount).any { t ->
        Intersector.isPointInTriangle(
                px, py,
                data[6 * t], data[6 * t + 1],
                data[6 * t + 2], data[6 * t + 3],
                data[6 * t + 4], data[6 * t + 5]
        )
    }
}