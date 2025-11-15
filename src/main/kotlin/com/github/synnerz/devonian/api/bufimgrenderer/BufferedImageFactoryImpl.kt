package com.github.synnerz.devonian.api.bufimgrenderer

import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.*
import java.util.*

class BufferedImageFactoryImpl : BufferedImageFactory {
    private var lastW = -1
    private var lastH = -1
    private var raster: WritableRaster? = null

    override fun create(w: Int, h: Int): BufferedImage {
        if (raster != null && w == lastW && h == lastH) {
            val bytes = (raster!!.dataBuffer as DataBufferByte).data
            Arrays.fill(bytes, 0)
        } else raster = Raster.createInterleavedRaster(
            DataBufferByte.TYPE_BYTE,
            w, h, w * 4,
            4, intArrayOf(0, 1, 2, 3),
            null
        )

        return BufferedImage(COLOR_MODEL, raster, false, null)
    }

    companion object {
        val COLOR_MODEL = ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB),
            true, false, Transparency.TRANSLUCENT,
            DataBufferByte.TYPE_BYTE
        )
        val BLANK_RASTER = Raster.createInterleavedRaster(
            DataBufferByte.TYPE_BYTE,
            1, 1, 4,
            4, intArrayOf(0, 1, 2, 3),
            null
        )
        val BLANK_IMAGE = BufferedImage(COLOR_MODEL, BLANK_RASTER, false, null)
    }
}