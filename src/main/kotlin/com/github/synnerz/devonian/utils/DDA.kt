package com.github.synnerz.devonian.utils

import net.minecraft.util.math.BlockPos
import java.util.*
import kotlin.math.floor
import kotlin.math.sign
import kotlin.math.sqrt

class DDA(x: Double, y: Double, z: Double, dx: Double, dy: Double, dz: Double) :
    Enumeration<BlockPos> {
    private var x = floor(x) as Int
    private var y = floor(y) as Int
    private var z = floor(z) as Int
    private val mag = sqrt(dx * dx + dy * dy + dz * dz)
    private val sx: Int
    private val sy: Int
    private val sz: Int
    private val tdx: Double
    private val tdy: Double
    private val tdz: Double
    private var tmx: Double
    private var tmy: Double
    private var tmz: Double

    private var t = 0.0

    init {
        val dx1 = dx / mag
        val dy1 = dy / mag
        val dz1 = dz / mag
        sx = sign(dx1) as Int
        sy = sign(dy1) as Int
        sz = sign(dz1) as Int
        tdx = sx / dx1
        tdy = sy / dy1
        tdz = sz / dz1
        tmx = if (dx1 == 0.0) Double.POSITIVE_INFINITY else (this.x + (if (dx1 > 0.0) 1.0 else 0.0) - x) / dx1
        tmy = if (dy1 == 0.0) Double.POSITIVE_INFINITY else (this.y + (if (dy1 > 0.0) 1.0 else 0.0) - y) / dy1
        tmz = if (dz1 == 0.0) Double.POSITIVE_INFINITY else (this.z + (if (dz1 > 0.0) 1.0 else 0.0) - z) / dz1
    }

    override fun hasMoreElements(): Boolean {
        return t <= mag
    }

    override fun nextElement(): BlockPos {
        val bp = BlockPos(x, y, z)

        if (tmx < tmy && tmx < tmz) {
            x += sx
            t = tmx
            tmx += tdx
        } else if (tmy < tmz) {
            y += sy
            t = tmy
            tmy += tdy
        } else {
            z += sz
            t = tmz
            tmz += tdz
        }

        return bp
    }
}