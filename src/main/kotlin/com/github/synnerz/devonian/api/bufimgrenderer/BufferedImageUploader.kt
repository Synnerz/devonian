package com.github.synnerz.devonian.api.bufimgrenderer

import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.TextureFormat
import net.minecraft.client.texture.AbstractTexture
import net.minecraft.client.texture.GlTexture
import org.lwjgl.opengl.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.nio.IntBuffer


class BufferedImageUploader(val name: String) : AbstractTexture() {
    var w: Int = 0
    var h: Int = 0
    var texId: Int = -1
    var pboId: Int = -1

    private fun create(img: BufferedImage) {
        w = img.width
        h = img.height
        glTexture = RenderSystem.getDevice().createTexture(name, TextureFormat.RGBA8, w, h, 1)
        texId = (glTexture as GlTexture).glId
        GlStateManager._bindTexture(texId)
        GlStateManager._texImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA,
            w,
            h,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            null as IntBuffer?
        )
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0)
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0)
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0)
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0)

        pboId = GlStateManager._glGenBuffers()
        GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, pboId)
        GlStateManager._glBufferData(GL21.GL_PIXEL_UNPACK_BUFFER, w * h * 4L, GL15.GL_STREAM_DRAW)
        GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0)
    }

    fun upload(img: BufferedImage) {
        val w = img.width
        val h = img.height

        if (texId == -1) create(img)
        else if (w != this.w || h != this.h) {
            destroy()
            create(img)
        }

        GlStateManager._bindTexture(texId)

        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, w)
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0)
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0)
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4)

        val pixels = (img.raster.dataBuffer as DataBufferByte).data
        GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, pboId)

        val buf = GL15.glMapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, GL15.GL_WRITE_ONLY, w * h * 4L, null)
        if (buf != null) {
            buf.put(pixels)
            GL15.glUnmapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER)
        }

        GlStateManager._texSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0)

        GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0)
    }

    private fun destroy() {
        if (texId == -1) return
        GlStateManager._deleteTexture(texId)
        if (pboId != -1) GlStateManager._glDeleteBuffers(pboId)
        pboId = -1
        texId = pboId
    }
}
