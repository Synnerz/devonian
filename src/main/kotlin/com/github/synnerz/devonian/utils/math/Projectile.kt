package com.github.synnerz.devonian.utils.math

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.mixin.accessor.LocalPlayerAccessor
import net.minecraft.world.entity.Pose
import net.minecraft.world.phys.Vec3
import java.util.BitSet
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

    fun aim(
        ttl: Double,
        path: Array<Vec3>,
        prevBestTarget: Int,
        err: Double,
        g: Double,
        v: Double,
        d: Double,
        h: Boolean,
        xo: Double = 0.0,
        yo: Double = 0.0,
        zo: Double = 0.0,
    ): Pair<Int, ProjectileData>? {
        val visited = BitSet(path.size)
        visited.set(prevBestTarget)
        val p = Devonian.minecraft.player ?: return null
        val pa = p as? LocalPlayerAccessor ?: return null
        val x = pa.lastXClient + xo
        val y = pa.lastYClient + yo + (if (p.pose == Pose.CROUCHING) 1.54f else p.eyeHeight)
        val z = pa.lastZClient + zo

        var bestI = prevBestTarget
        var best = solve(
            path[prevBestTarget].x - x,
            path[prevBestTarget].y - y,
            path[prevBestTarget].z - z,
            err, g, v, d, h,
        )
        var bestO = abs(best.ticks - ttl - prevBestTarget)
        if (bestO.isNaN()) bestO = Double.POSITIVE_INFINITY

        var dir = 1
        var swapped = false
        var i = prevBestTarget
        while (true) {
            val next = i + dir
            var shouldSwap = false
            if (next in path.indices && !visited.get(next)) {
                visited.set(next)
                val data = solve(
                    path[next].x - x,
                    path[next].y - y,
                    path[next].z - z,
                    err, g, v, d, h,
                )
                val o = abs(data.ticks - ttl - next)
                if (o < bestO) {
                    bestI = next
                    bestO = o
                    best = data
                    i = next
                } else shouldSwap = true
            } else shouldSwap = true

            if (shouldSwap) {
                if (swapped) break
                swapped = true
                dir = -dir
            }
        }

        return Pair(if (bestO.isFinite()) bestI else 0, best)
    }
}