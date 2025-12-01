package com.github.synnerz.devonian.utils.math

import org.ejml.simple.SimpleMatrix
import kotlin.math.*

object MathUtils {
    fun toPolynomial(coeffs: DoubleArray): (x: Double) -> Double {
        return { x -> coeffs.reduceRight { v, a -> a * x + v } }
    }

    fun polyRegression(dim: Int, x: DoubleArray, y: DoubleArray): DoubleArray? {
        if (x.size != y.size) throw IllegalArgumentException("unequal sized inputs")
        val X = SimpleMatrix(Array(x.size) { i -> DoubleArray(dim + 1) { p -> x[i].pow(p) } })
        val Y = SimpleMatrix(Array(y.size) { i -> doubleArrayOf(y[i]) })

        val XT = X.transpose()
        val XT_X = XT.mult(X)
        val XT_Y = XT.mult(Y)

        try {
            val INV_XT_X = XT_X.invert()
            val C = INV_XT_X.mult(XT_Y)
            return DoubleArray(C.numRows) { C[it, 0] }
        } catch (ignored: Exception) {
            return null
        }
    }

    fun convergeHalfInterval(
        func: (x: Double) -> Double,
        target: Double,
        min: Double,
        max: Double,
        increasing: Boolean,
        iters: Int = 100,
        eps: Double = 1e-10
    ): Double {
        var y: Double
        var x: Double
        var l = min
        var r = max
        var i = iters
        do {
            x = (l + r) * 0.5
            y = func(x)
            if (increasing == (y < target)) l = x
            else r = x
        } while (--i >= 0 && abs(target - y) < eps)
        return x
    }

    fun rescale(v: Double, oldMin: Double, oldMax: Double, newMin: Double, newMax: Double) =
        (v - oldMin) / (oldMax - oldMin) * (newMax - newMin) + newMin

    // https://stackoverflow.com/a/37716142
    private val binomLookup = mutableListOf(
        intArrayOf(1),
        intArrayOf(1, 1),
        intArrayOf(1, 2, 1),
        intArrayOf(1, 3, 3, 1),
        intArrayOf(1, 4, 6, 4, 1),
        intArrayOf(1, 5, 10, 10, 5, 1),
        intArrayOf(1, 6, 15, 20, 15, 6, 1),
        intArrayOf(1, 7, 21, 35, 35, 21, 7, 1),
        intArrayOf(1, 8, 28, 56, 70, 56, 28, 8, 1),
    )

    fun binomial(n: Int, k: Int): Int {
        while (n >= binomLookup.size) {
            val s = binomLookup.size
            val prev = binomLookup[s - 1]
            binomLookup.add(IntArray(s + 1) {
                when (it) {
                    0, s -> 1
                    else -> prev[it - 1] + prev[it]
                }
            })
        }
        return binomLookup[n][k]
    }

    val FIRST_100_PRIMES =
        intArrayOf(
            2, 3, 5, 7, 11, 13, 17, 19, 23, 29,
            31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
            73, 79, 83, 89, 97, 101, 103, 107, 109, 113,
            127, 131, 137, 139, 149, 151, 157, 163, 167, 173,
            179, 181, 191, 193, 197, 199, 211, 223, 227, 229,
            233, 239, 241, 251, 257, 263, 269, 271, 277, 281,
            283, 293, 307, 311, 313, 317, 331, 337, 347, 349,
            353, 359, 367, 373, 379, 383, 389, 397, 401, 409,
            419, 421, 431, 433, 439, 443, 449, 457, 461, 463,
            467, 479, 487, 491, 499, 503, 509, 521, 523, 541
        )

    fun ceilPow2(num: Int, bits: Int): Int {
        val mask = (1 shl max(0, 31 - Integer.numberOfLeadingZeros(num) - bits)) - 1
        return (num + mask) and mask.inv()
    }

    fun lerp(f: Double, o: Double, n: Double) = (n - o) * f.coerceIn(0.0 .. 1.0) + o

    fun lerpAngle(f: Double, ao: Double, an: Double): Double {
        var o = ao % (2.0 * PI)
        var n = an % (2.0 * PI)
        if (o < 0.0) o += 2.0 * PI
        if (n < 0.0) n += 2.0 * PI
        if (n - o > PI) n -= 2.0 * PI
        if (o - n > PI) o -= 2.0 * PI
        return lerp(f, o, n)
    }

    fun rotate(x: Double, y: Double, z: Double, t: Double, p: Double, r: Double): Triple<Double, Double, Double> {
        val ct = cos(t)
        val st = sin(t)
        val cp = cos(p)
        val sp = sin(p)
        val cr = cos(r)
        val sr = sin(r)
        return Triple(
            x * ct * cp +
            z * (ct * sp * sr - st * cr) +
            y * (ct * sp * cr + st * sr),

            x * -sp +
            z * cp * sr +
            y * cp * cr,

            x * st * cp +
            z * (st * sp * sr + ct * cr) +
            y * (st * sp * cr - ct * sr),
        )
    }
}