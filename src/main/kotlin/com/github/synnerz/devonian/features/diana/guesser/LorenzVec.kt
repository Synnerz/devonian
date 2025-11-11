package com.github.synnerz.devonian.features.diana.guesser

import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

// Credits: https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/utils/LorenzVec.kt
@Suppress("MemberVisibilityCanBePrivate")
data class LorenzVec(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    fun distance(other: LorenzVec): Double = distanceSq(other).pow(0.5)

    fun distanceSq(other: LorenzVec): Double {
        val dx = other.x - x
        val dy = other.y - y
        val dz = other.z - z
        return (dx * dx + dy * dy + dz * dz)
    }

    operator fun plus(other: LorenzVec) = LorenzVec(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: LorenzVec) = LorenzVec(x - other.x, y - other.y, z - other.z)

    operator fun times(other: LorenzVec) = LorenzVec(x * other.x, y * other.y, z * other.z)
    operator fun times(other: Double) = LorenzVec(x * other, y * other, z * other)
    operator fun times(other: Int) = LorenzVec(x * other, y * other, z * other)

    operator fun div(other: LorenzVec) = LorenzVec(x / other.x, y / other.y, z / other.z)
    operator fun div(other: Double) = LorenzVec(x / other, y / other, z / other)

    fun add(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): LorenzVec =
        LorenzVec(this.x + x, this.y + y, this.z + z)

    fun add(x: Int = 0, y: Int = 0, z: Int = 0): LorenzVec = LorenzVec(this.x + x, this.y + y, this.z + z)

    fun lengthSquared(): Double = x * x + y * y + z * z
    fun length(): Double = sqrt(lengthSquared())

    fun toDoubleArray(): Array<Double> = arrayOf(x, y, z)

    fun roundToBlock() = LorenzVec(floor(x), floor(y), floor(z))

    fun blockCenter() = roundToBlock().add(0.5, 0.5, 0.5)

    fun scale(scalar: Double): LorenzVec = LorenzVec(scalar * x, scalar * y, scalar * z)

    private operator fun div(i: Number): LorenzVec = LorenzVec(x / i.toDouble(), y / i.toDouble(), z / i.toDouble())

    private val normX = if (x == 0.0) 0.0 else x
    private val normY = if (y == 0.0) 0.0 else y
    private val normZ = if (z == 0.0) 0.0 else z

    override fun equals(other: Any?): Boolean {
        if (other is LorenzVec) {
            val v2: LorenzVec = other
            if (this.x == v2.x && this.y == v2.y && this.z == v2.z) {
                return true
            }
        }
        return false
    }

    override fun hashCode() = 31 * (31 * normX.hashCode() + normY.hashCode()) + normZ.hashCode()

    companion object {
        fun List<Double>.toLorenzVec(): LorenzVec {
            if (size != 3) error("Can not transform a list of size $size to LorenzVec")

            return LorenzVec(this[0], this[1], this[2])
        }
    }
}