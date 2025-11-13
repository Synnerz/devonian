package com.github.synnerz.devonian.features.diana.guesser

import com.github.synnerz.devonian.features.diana.guesser.LorenzVec.Companion.toLorenzVec
import com.github.synnerz.devonian.utils.Matrix
import kotlin.math.pow
import kotlin.math.*

// Credits: https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/utils/PolynomialFitter.kt

class PolynomialFitter(private val degree: Int) {
    private val xPointMatrix: ArrayList<DoubleArray> = ArrayList()
    private val yPoints: ArrayList<DoubleArray> = ArrayList()

    fun addPoint(x: Double, y: Double) {
        yPoints.add(doubleArrayOf(y))
        val xArray = DoubleArray(degree + 1)
        for (i in xArray.indices) {
            xArray[i] = x.pow(i)
        }
        xPointMatrix.add(xArray)
    }

    fun fit(): DoubleArray {
        val xMatrix = Matrix(xPointMatrix.toTypedArray())
        val yMatrix = Matrix(yPoints.toTypedArray())
        val xMatrixTransposed = xMatrix.transpose()
        return ((xMatrixTransposed * xMatrix).inverse() * xMatrixTransposed * yMatrix).transpose()[0]
    }

    fun reset() {
        xPointMatrix.clear()
        yPoints.clear()
    }
}

open class BezierFitter(private val degree: Int) {
    val points: MutableList<LorenzVec> = mutableListOf()
    private val fitters = arrayOf(PolynomialFitter(degree), PolynomialFitter(degree), PolynomialFitter(degree))
    fun addPoint(point: LorenzVec) {
        require(point.x.isFinite() && point.y.isFinite() && point.z.isFinite()) { "Points may not contain NaN!" }
        val locationArray = point.toDoubleArray()
        for ((i, fitter) in fitters.withIndex()) {
            fitter.addPoint(points.size.toDouble(), locationArray[i])
        }
        points.add(point)
        lastCurve = null
    }

    fun getLastPoint(): LorenzVec? {
        return points.lastOrNull()
    }

    fun isEmpty(): Boolean {
        return points.isEmpty()
    }

    fun count() = points.size

    private var lastCurve: BezierCurve? = null
    fun fit(): BezierCurve? {
        // A Degree n polynomial can be solved with n+1 unique points
        // The Bézier curve used is a degree n, so n + 1 points are needed to solve
        if (points.size <= degree) return null

        if (lastCurve != null) return lastCurve

        val coefficients = fitters.map { it.fit() }
        lastCurve = BezierCurve(coefficients)
        return lastCurve
    }

    fun reset() {
        points.clear()
        fitters.map { it.reset() }
        lastCurve = null
    }
}

class ParticlePathBezierFitter(degree: Int) : BezierFitter(degree) {
    fun solve(): LorenzVec? {
        val bezierCurve = fit() ?: return null

        val startPointDerivative = bezierCurve.derivativeAt(0.0)

        // How far away from the first point the control point is
        val controlPointDistance = computePitchWeight(startPointDerivative)

        val t = 3 * controlPointDistance / startPointDerivative.length()

        return bezierCurve.at(t)
    }

    private fun computePitchWeight(derivative: LorenzVec) = sqrt(24 * sin(getPitchFromDerivative(derivative) - PI) + 25)

    private fun getPitchFromDerivative(derivative: LorenzVec): Double {
        val xzLength = sqrt(derivative.x.pow(2) + derivative.z.pow(2))
        val pitchRadians = -atan2(derivative.y, xzLength)
        // Solve y = atan2(sin(x) - 0.75, cos(x)) for x from y
        var guessPitch = pitchRadians
        var resultPitch = atan2(sin(guessPitch) - 0.75, cos(guessPitch))
        var windowMax = PI / 2
        var windowMin = -PI / 2
        repeat(100) {
            if (resultPitch < pitchRadians) {
                windowMin = guessPitch
                guessPitch = (windowMin + windowMax) / 2
            } else {
                windowMax = guessPitch
                guessPitch = (windowMin + windowMax) / 2
            }
            resultPitch = atan2(sin(guessPitch) - 0.75, cos(guessPitch))
            if (resultPitch == pitchRadians) return guessPitch
        }
        return guessPitch
    }
}

class BezierCurve(private val coefficients: List<DoubleArray>) {
    init {
        require(coefficients.size == 3) { "Coefficients must be for a 3d curve!" }
    }

    fun derivativeAt(t: Double): LorenzVec {
        return coefficients.map {
            var result = 0.0
            val reversed = it.reversedArray().dropLast(1)
            for ((i, coeff) in reversed.withIndex()) {
                result = result * t + coeff * (reversed.size - i)
            }
            result
        }.toLorenzVec()
    }

    fun at(t: Double): LorenzVec {
        return coefficients.map {
            var result = 0.0
            val reversed = it.reversed()
            for (coeff in reversed) {
                result = result * t + coeff
            }
            result
        }.toLorenzVec()
    }
}