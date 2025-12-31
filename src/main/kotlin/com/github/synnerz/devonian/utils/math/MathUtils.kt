package com.github.synnerz.devonian.utils.math

import kotlin.math.*

object MathUtils {
    fun toPolynomial(coeffs: DoubleArray): (x: Double) -> Double {
        return { x -> coeffs.reduceRight { v, a -> a * x + v } }
    }

    fun polyRegression(dim: Int, x: DoubleArray, y: DoubleArray): DoubleArray? {
        if (x.size != y.size) throw IllegalArgumentException("unequal sized inputs")
        val X = Matrix(Array(x.size) { i -> DoubleArray(dim + 1) { p -> x[i].pow(p) } })
        val Y = Matrix(Array(y.size) { i -> doubleArrayOf(y[i]) })

        val XT = X.transpose()
        val XT_X = XT.mult(X)
        val XT_Y = XT.mult(Y)

        try {
            val INV_XT_X = XT_X.invert()
            val C = INV_XT_X.mult(XT_Y)
            return DoubleArray(C.rows) { C[it][0] }
        } catch (_: Exception) {
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
        } while (--i >= 0 && abs(target - y) > eps)
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

    // fastapprox v0.3.2
    /*=====================================================================*
     *                   Copyright (C) 2011 Paul Mineiro                   *
     * All rights reserved.                                                *
     *                                                                     *
     * Redistribution and use in source and binary forms, with             *
     * or without modification, are permitted provided that the            *
     * following conditions are met:                                       *
     *                                                                     *
     *     * Redistributions of source code must retain the                *
     *     above copyright notice, this list of conditions and             *
     *     the following disclaimer.                                       *
     *                                                                     *
     *     * Redistributions in binary form must reproduce the             *
     *     above copyright notice, this list of conditions and             *
     *     the following disclaimer in the documentation and/or            *
     *     other materials provided with the distribution.                 *
     *                                                                     *
     *     * Neither the name of Paul Mineiro nor the names                *
     *     of other contributors may be used to endorse or promote         *
     *     products derived from this software without specific            *
     *     prior written permission.                                       *
     *                                                                     *
     * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND              *
     * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,         *
     * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES               *
     * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE             *
     * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER               *
     * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                 *
     * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES            *
     * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE           *
     * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR                *
     * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF          *
     * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT           *
     * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY              *
     * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE             *
     * POSSIBILITY OF SUCH DAMAGE.                                         *
     *                                                                     *
     * Contact: Paul Mineiro <paul@mineiro.com>                            *
     *=====================================================================*/
    fun fastLambertW0(x: Float): Float {
        val threshold = 2.26445f

        val c = if (x < threshold) 1.5468656f else 1.0f
        val d = if (x < threshold) 2.250367f else 0.0f
        val a = if (x < threshold) -0.73776996f else 0.0f

        val logterm = fastLog(c * x + d)
        val loglogterm = fastLog(logterm)

        val minusw = -a - logterm + loglogterm - loglogterm / logterm
        val expminusw = fastExp(minusw)
        val xexpminusw = x * expminusw
        val pexpminusw = xexpminusw - minusw

        return (2.0f * xexpminusw - minusw * (4.0f * xexpminusw - minusw * pexpminusw)) / (2.0f + pexpminusw * (2.0f - minusw))
    }

    fun fastLog2(x: Float): Float {
        val vx = x.toRawBits()
        val mx = Float.fromBits((vx and 0x007FFFFF) or 0x3f000000)

        return vx * 1.1920929E-7f - 124.22552f - 1.4980303f * mx - 1.72588f / (0.35208872f + mx)
    }

    fun fastLog(x: Float): Float {
        return 0.6931472f * fastLog2(x)
    }

    fun fastPow2(p: Float): Float {
        val offset = if (p < 0) 1.0f else 0.0f
        val clipp = if (p < -126) -126.0f else p
        val w = clipp.toInt()
        val z = clipp - w + offset

        return Float.fromBits(((1 shl 23) * (clipp + 121.274055f + 27.728024f / (4.8425255f - z) - 1.4901291f * z)).toInt())
    }

    fun fastExp(p: Float): Float {
        return fastPow2(1.442695f * p)
    }



    // http://doi.org/10.1016/j.cpc.2012.07.008
    // https://doi.org/10.1016/S0378-4754(02)00051-4
    fun lambertW0(x: Double): Double {
        if (x < -1.0 / Math.E) return Double.NaN
        return if (x == 0.0) 0.0 else lambertWFritschIteration(
            x,
            fastLambertW0(x.toFloat()).toDouble(),
            1.0,
            Double.MIN_VALUE
        )
    }

    fun lambertW1(x: Double): Double {
        if (x < -1.0 / Math.E || 0.0 <= x) return Double.NaN
        return lambertWFritschIteration(x, lambertW1Guess(x), 1.0, Double.MIN_VALUE)
    }

    fun lambertWFritschIteration(x: Double, W: Double, iter: Double, eps: Double): Double {
        var W = W
        var iter = iter
        var r = abs(W - ln(abs(x)) + ln(abs(W)))

        while (r > eps && --iter >= 0) {
            val z = ln(x / W) - W
            val q = 2 * (1 + W) * (1 + W + (2.0 / 3.0) * z)
            val eps_term = z * (q - z) / ((1 + W) * (q - 2 * z))
            W *= (1 + eps_term)

            r = abs(W - ln(abs(x)) + ln(abs(W)))
        }

        return if (java.lang.Double.isNaN(W)) 0.0 else W
    }

    fun lambertW1Guess(x: Double): Double {
        val M1 = 0.3361
        val M2 = -0.0042
        val M3 = -0.0201

        val sigma = -1 - ln(-x)
        val sqrt_sigma = sqrt(sigma)
        val expr = (M1 * sqrt_sigma / 2) / (1 + M2 * sigma * exp(M3 * sqrt_sigma))
        return -1 - sigma - (2 / M1) * (1 - 1 / (1 + expr))
    }


    fun fitLine3D(x: List<Double>, y: List<Double>, z: List<Double>): Array<DoubleArray> {
        if (x.size != y.size || y.size != z.size) throw IllegalArgumentException("inputs not same size")

        val mx = x.sum() / x.size
        val my = y.sum() / y.size
        val mz = z.sum() / z.size

        val XT = Matrix(arrayOf(x.toDoubleArray(), y.toDoubleArray(), z.toDoubleArray()))
        val PX = Matrix(Array(x.size) { doubleArrayOf(x[it] - mx, y[it] - my, z[it] - mz) })
        val M = XT.mult(PX)

        var b_k = Matrix(arrayOf(doubleArrayOf(1.0), doubleArrayOf(0.0), doubleArrayOf(0.0)))
        var iter = 50
        while (--iter >= 0) {
            val v = M.mult(b_k)

            val vx = v[0][0]
            val vy = v[1][0]
            val vz = v[2][0]
            val l2norm = sqrt(vx * vx + vy * vy + vz * vz)
            if (l2norm < 1e-12) break

            val b_k1 = v.scale(1.0 / l2norm)

            var diff = 0.0
            for (r in 0 until 3) {
                diff = max(diff, abs(b_k[r][0] - b_k1[r][0]))
            }

            b_k = b_k1

            if (diff < 1e-10) break
        }

        return arrayOf(
            doubleArrayOf(mx, b_k[0][0]),
            doubleArrayOf(my, b_k[1][0]),
            doubleArrayOf(mz, b_k[2][0]),
        )
    }

    data class LinearRegressionResult(val r: Double, val b: Double, val a: Double)

    fun linReg(x: List<Double>, y: List<Double>): LinearRegressionResult? {
        if (x.size != y.size) throw IllegalArgumentException("inputs not same size")
        if (x.size < 2) return null

        val mx = x.sum() / x.size
        val my = y.sum() / y.size

        var xStd = 0.0
        var yStd = 0.0
        var r = 0.0
        for (i in x.indices) {
            val dx = (x[i] - mx)
            val dy = (y[i] - my)
            xStd += dx * dx
            yStd += dy * dy
            r += dx * dy
        }

        xStd = sqrt(xStd)
        yStd = sqrt(yStd)
        if (xStd == 0.0) return null
        if (yStd == 0.0) return LinearRegressionResult(1.0, 0.0, my)
        r /= (xStd * yStd)

        val b = r * yStd / xStd
        val a = my - b * mx

        return LinearRegressionResult(r, b, a)
    }
}