package com.github.synnerz.devonian.utils.math

import kotlin.math.*

object Projectile {
    data class ProjectileData(val theta: Double, val phi: Double, val ticks: Double) {
        override fun toString(): String {
            return "%.1f %.1f %.2f".format(theta * 180 / Math.PI, phi * 180 / Math.PI, ticks / 20)
        }
    }

    private const val NEG_INV_E = -1.0 / E

    fun solve(
        dx: Double, dy: Double, dz: Double,
        eps: Double,
        a: Double, v: Double, d: Double,
        high: Boolean
    ): ProjectileData {
        if (d == 1.0) throw IllegalArgumentException(":(")

        val theta = atan2(dz, dx)
        val R = hypot(dx, dz)

        val f = 1.0 / (d - 1.0)
        val lnd = ln(d)
        val inv_a = 1.0 / a
        val inv_lnd = 1.0 / lnd

        var A = v + a * f
        var r = -(dy * (d - 1.0) + A) * inv_a
        var B = -A * lnd * exp(lnd * r) * inv_a
        if (B < NEG_INV_E) return ProjectileData(Double.NaN, Double.NaN, Double.NaN)

        if (dy > 0.0 && !high) {
            var searchL = 0.0
            var searchR = PI / 2.0
            var t = 0.0

            while (searchR - searchL > eps) {
                val m = (searchL + searchR) * 0.5
                A = v * cos(m) + a * f
                r = -(dy * (d - 1.0) + A) * inv_a
                B = -A * lnd * exp(lnd * r) * inv_a
                if (B < NEG_INV_E) searchR = m
                else {
                    t = r - inv_lnd * MathUtils.lambertW1(B)
                    val x = v * sin(m) * (exp(lnd * t) - 1.0) * f
                    if (x < R) searchL = m
                    else searchR = m
                }
            }

            return ProjectileData(theta, (searchL + searchR) * 0.5, t)
        }

        var maxDomain = if (dy > 0.0) PI * 0.5 else PI
        var searchL = 0.0
        var searchR = maxDomain
        var x1 = 0.0
        var x2 = 0.0

        while (true) {
            val windowSize = searchR - searchL
            if (windowSize < eps) break

            val m1 = windowSize / 3.0 + searchL
            A = v * cos(m1) + a * f
            r = -(dy * (d - 1.0) + A) * inv_a
            B = -A * lnd * exp(lnd * r) * inv_a
            if (B < NEG_INV_E) {
                searchR = m1
                maxDomain = m1
            } else {
                x1 = v * sin(m1) * (exp(lnd * (r - inv_lnd * MathUtils.lambertW0(B))) - 1.0) * f
                val m2 = windowSize * 2.0 / 3.0 + searchL
                A = v * cos(m2) + a * f
                r = -(dy * (d - 1.0) + A) * inv_a
                B = -A * lnd * exp(lnd * r) * inv_a
                if (B < NEG_INV_E) {
                    searchR = m2
                    maxDomain = m2
                } else {
                    x2 = v * sin(m2) * (exp(lnd * (r - inv_lnd * MathUtils.lambertW0(B))) - 1.0) * f
                    if (x1 < x2) searchL = m1
                    else searchR = m2
                }
            }
        }

        if (R > (x1 + x2) * 0.5) return ProjectileData(Double.NaN, Double.NaN, Double.NaN)

        val peak = (searchL + searchR) * 0.5
        searchL = if (high) 0.0 else peak
        searchR = if (high) peak else maxDomain
        var t = Double.NaN

        while (searchR - searchL > eps) {
            val m = (searchL + searchR) * 0.5
            A = v * cos(m) + a * f
            r = -(dy * (d - 1.0) + A) * inv_a
            B = -A * lnd * exp(lnd * r) * inv_a
            if (B < NEG_INV_E) searchR = m
            else {
                t = r - inv_lnd * MathUtils.lambertW0(B)
                x1 = v * sin(m) * (exp(lnd * t) - 1.0) * f
                if (x1 < R == high) searchL = m
                else searchR = m
            }
        }

        return ProjectileData(theta, (searchL + searchR) * 0.5, t)
    }
}
