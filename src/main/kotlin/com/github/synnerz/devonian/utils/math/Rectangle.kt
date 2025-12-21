package com.github.synnerz.devonian.utils.math

import kotlin.math.max
import kotlin.math.min

data class Rectangle(val x1: Double, val y1: Double, val x2: Double, val y2: Double) {
    fun isEmpty() = x1 == x2 || y1 == y2

    fun intersects(r: Rectangle) =
        x1 < r.x2 && x2 > r.x1 &&
        y1 < r.y2 && y2 > r.y1

    fun subtract(r: Rectangle): List<Rectangle> {
        if (!intersects(r)) return emptyList()

        val arr = mutableListOf<Rectangle>()

        if (x1 < r.x1) arr.add(Rectangle(x1, y1, r.x1, y2))
        if (x2 > r.x2) arr.add(Rectangle(r.x2, y1, x2, y2))
        if (y1 < r.y1) arr.add(Rectangle(max(x1, r.x1), y1, min(x2, r.x2), r.y1))
        if (y2 > r.y2) arr.add(Rectangle(max(x1, r.x1), r.y2, min(x2, r.x2), y2))

        return arr
    }
}