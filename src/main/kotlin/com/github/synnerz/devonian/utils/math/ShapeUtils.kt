package com.github.synnerz.devonian.utils.math

import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.VoxelShape

object ShapeUtils {
    fun getFaces(shape: VoxelShape): FloatArray {
        val aabbs = shape.toAabbs()

        val xDict = mutableMapOf<Int, MutableList<AABB>>()
        val yDict = mutableMapOf<Int, MutableList<AABB>>()
        val zDict = mutableMapOf<Int, MutableList<AABB>>()
        aabbs.forEach {
            xDict.getOrPut((it.minX * 1000.0).toInt()) { mutableListOf() }.add(it)
            yDict.getOrPut((it.minY * 1000.0).toInt()) { mutableListOf() }.add(it)
            zDict.getOrPut((it.minZ * 1000.0).toInt()) { mutableListOf() }.add(it)
            xDict.getOrPut((it.maxX * 1000.0).toInt()) { mutableListOf() }.add(it)
            yDict.getOrPut((it.maxY * 1000.0).toInt()) { mutableListOf() }.add(it)
            zDict.getOrPut((it.maxZ * 1000.0).toInt()) { mutableListOf() }.add(it)
        }

        val shared = aabbs.map { box ->
            val x1Arr = xDict[(box.minX * 1000.0).toInt()]!!
            val y1Arr = yDict[(box.minY * 1000.0).toInt()]!!
            val z1Arr = zDict[(box.minZ * 1000.0).toInt()]!!
            val x2Arr = xDict[(box.maxX * 1000.0).toInt()]!!
            val y2Arr = yDict[(box.maxY * 1000.0).toInt()]!!
            val z2Arr = zDict[(box.maxZ * 1000.0).toInt()]!!
            val x1Shared = x1Arr.filter {
                it !== box &&
                box.minY < it.maxY && box.maxY > it.minY &&
                box.minZ < it.maxZ && box.maxZ > it.minZ
            }
            val y1Shared = y1Arr.filter {
                it !== box &&
                box.minX < it.maxX && box.maxX > it.minX &&
                box.minZ < it.maxZ && box.maxZ > it.minZ
            }
            val z1Shared = z1Arr.filter {
                it !== box &&
                box.minX < it.maxX && box.maxX > it.minX &&
                box.minY < it.maxY && box.maxY > it.minY
            }
            val x2Shared = x2Arr.filter {
                it !== box &&
                box.minY < it.maxY && box.maxY > it.minY &&
                box.minZ < it.maxZ && box.maxZ > it.minZ
            }
            val y2Shared = y2Arr.filter {
                it !== box &&
                box.minX < it.maxX && box.maxX > it.minX &&
                box.minZ < it.maxZ && box.maxZ > it.minZ
            }
            val z2Shared = z2Arr.filter {
                it !== box &&
                box.minX < it.maxX && box.maxX > it.minX &&
                box.minY < it.maxY && box.maxY > it.minY
            }

            return@map arrayOf(x1Shared, y1Shared, z1Shared, x2Shared, y2Shared, z2Shared)
        }

        val arr = ArrayList<Float>()
        aabbs.forEachIndexed { i, box ->
            val x1Shared = shared[i][0]
            val y1Shared = shared[i][1]
            val z1Shared = shared[i][2]
            val x2Shared = shared[i][3]
            val y2Shared = shared[i][4]
            val z2Shared = shared[i][5]

            val x1 = box.minX.toFloat()
            val y1 = box.minY.toFloat()
            val z1 = box.minZ.toFloat()
            val x2 = box.maxX.toFloat()
            val y2 = box.maxY.toFloat()
            val z2 = box.maxZ.toFloat()

            var x1Rects = listOf(Rectangle(box.minY, box.minZ, box.maxY, box.maxZ))
            x1Shared.forEach { b ->
                x1Rects = x1Rects.flatMap { it.subtract(Rectangle(b.minY, b.minZ, b.maxY, b.maxZ)) }
            }

            var y1Rects = listOf(Rectangle(box.minX, box.minZ, box.maxX, box.maxZ))
            y1Shared.forEach { b ->
                y1Rects = y1Rects.flatMap { it.subtract(Rectangle(b.minX, b.minZ, b.maxX, b.maxZ)) }
            }

            var z1Rects = listOf(Rectangle(box.minX, box.minY, box.maxX, box.maxY))
            z1Shared.forEach { b ->
                z1Rects = z1Rects.flatMap { it.subtract(Rectangle(b.minX, b.minY, b.maxX, b.maxY)) }
            }

            var x2Rects = listOf(Rectangle(box.minY, box.minZ, box.maxY, box.maxZ))
            x2Shared.forEach { b ->
                x2Rects = x2Rects.flatMap { it.subtract(Rectangle(b.minY, b.minZ, b.maxY, b.maxZ)) }
            }

            var y2Rects = listOf(Rectangle(box.minX, box.minZ, box.maxX, box.maxZ))
            y2Shared.forEach { b ->
                y2Rects = y2Rects.flatMap { it.subtract(Rectangle(b.minX, b.minZ, b.maxX, b.maxZ)) }
            }

            var z2Rects = listOf(Rectangle(box.minX, box.minY, box.maxX, box.maxY))
            z2Shared.forEach { b ->
                z2Rects = z2Rects.flatMap { it.subtract(Rectangle(b.minX, b.minY, b.maxX, b.maxY)) }
            }

            x1Rects.forEach {
                arr.add(x1); arr.add(it.x1.toFloat()); arr.add(it.y1.toFloat())
                arr.add(x1); arr.add(it.x1.toFloat()); arr.add(it.y2.toFloat())
                arr.add(x1); arr.add(it.x2.toFloat()); arr.add(it.y2.toFloat())
                arr.add(x1); arr.add(it.x2.toFloat()); arr.add(it.y1.toFloat())
            }
            y1Rects.forEach {
                arr.add(it.x1.toFloat()); arr.add(y1); arr.add(it.y1.toFloat())
                arr.add(it.x2.toFloat()); arr.add(y1); arr.add(it.y1.toFloat())
                arr.add(it.x2.toFloat()); arr.add(y1); arr.add(it.y2.toFloat())
                arr.add(it.x1.toFloat()); arr.add(y1); arr.add(it.y2.toFloat())
            }
            z1Rects.forEach {
                arr.add(it.x1.toFloat()); arr.add(it.y1.toFloat()); arr.add(z1)
                arr.add(it.x1.toFloat()); arr.add(it.y2.toFloat()); arr.add(z1)
                arr.add(it.x2.toFloat()); arr.add(it.y2.toFloat()); arr.add(z1)
                arr.add(it.x2.toFloat()); arr.add(it.y1.toFloat()); arr.add(z1)
            }
            x2Rects.forEach {
                arr.add(x2); arr.add(it.x1.toFloat()); arr.add(it.y1.toFloat())
                arr.add(x2); arr.add(it.x2.toFloat()); arr.add(it.y1.toFloat())
                arr.add(x2); arr.add(it.x2.toFloat()); arr.add(it.y2.toFloat())
                arr.add(x2); arr.add(it.x1.toFloat()); arr.add(it.y2.toFloat())
            }
            y2Rects.forEach {
                arr.add(it.x1.toFloat()); arr.add(y2); arr.add(it.y1.toFloat())
                arr.add(it.x1.toFloat()); arr.add(y2); arr.add(it.y2.toFloat())
                arr.add(it.x2.toFloat()); arr.add(y2); arr.add(it.y2.toFloat())
                arr.add(it.x2.toFloat()); arr.add(y2); arr.add(it.y1.toFloat())
            }
            z2Rects.forEach {
                arr.add(it.x1.toFloat()); arr.add(it.y1.toFloat()); arr.add(z2)
                arr.add(it.x2.toFloat()); arr.add(it.y1.toFloat()); arr.add(z2)
                arr.add(it.x2.toFloat()); arr.add(it.y2.toFloat()); arr.add(z2)
                arr.add(it.x1.toFloat()); arr.add(it.y2.toFloat()); arr.add(z2)
            }
        }

        return arr.toFloatArray()
    }
}