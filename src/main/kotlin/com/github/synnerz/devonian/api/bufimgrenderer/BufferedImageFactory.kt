package com.github.synnerz.devonian.api.bufimgrenderer

import com.mojang.blaze3d.platform.NativeImage
import org.lwjgl.system.MemoryUtil
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer
import java.awt.image.DataBufferByte
import java.awt.image.Raster
import java.awt.image.WritableRaster
import java.nio.ByteBuffer

class BufferedImageFactory {
    fun createJava(w: Int, h: Int): BufferedImage {
        val raster = Raster.createInterleavedRaster(
            DataBufferByte.TYPE_BYTE,
            w, h, w * 4,
            4, intArrayOf(0, 1, 2, 3),
            null
        )

        return BufferedImage(COLOR_MODEL, raster, false, null)
    }

    fun createNative(w: Int, h: Int): NativeBufferedImage {
        val p = MemoryUtil.nmemCalloc(1L, 4L * w * h)
        val ni = NativeImage(NativeImage.Format.RGBA, w, h, p)
        val buf = MemoryUtil.memByteBuffer(p, 4 * w * h)
        val db = DirectDataBuffer(buf, 4 * w * h)

        val raster = Raster.createInterleavedRaster(
            db,
            w, h, w * 4,
            4, intArrayOf(0, 1, 2, 3),
            null
        )

        return NativeBufferedImage(raster, ni)
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
        )!!
        val BLANK_IMAGE = BufferedImage(COLOR_MODEL, BLANK_RASTER, false, null)
    }
}

class NativeBufferedImage(raster: WritableRaster, val backing: NativeImage) : BufferedImage(
    BufferedImageFactory.COLOR_MODEL,
    raster,
    false,
    null,
)

class DirectDataBuffer(val buf: ByteBuffer, size: Int) : DataBuffer(TYPE_BYTE, size) {
    override fun getElem(bank: Int, i: Int): Int {
        if (bank != 0) throw UnsupportedOperationException()
        return buf.get(i + offset).toInt() and 0xFF
    }

    override fun getElem(i: Int): Int {
        return buf.get(i + offset).toInt() and 0xFF
    }

    override fun setElem(bank: Int, i: Int, `val`: Int) {
        if (bank != 0) throw UnsupportedOperationException()
        buf.put(i, `val`.toByte())
    }

    override fun setElem(i: Int, `val`: Int) {
        buf.put(i, `val`.toByte())
    }
}