package com.github.synnerz.devonian.utils

import kotlin.math.min

data class BoundingBox(val x: Double, val y: Double, val w: Double, val h: Double) {
    fun inBounds(px: Double, py: Double) =
        x <= px && px <= x + w &&
        y <= py && py <= y + h

    fun fitInside(box: BoundingBox): Pair<Double, BoundingBox> {
        val f = min(box.w / w, box.h / h)
        val w1 = w * f
        val h1 = h * f
        return Pair(f, BoundingBox(box.x + (box.w - w1) * 0.5, box.y + (box.h - h1) * 0.5, w1, h1))
    }

    fun centerInside(box: BoundingBox) = BoundingBox(
        box.x + 0.5 * (box.w - w),
        box.y + 0.5 * (box.h - h),
        w, h
    )
}