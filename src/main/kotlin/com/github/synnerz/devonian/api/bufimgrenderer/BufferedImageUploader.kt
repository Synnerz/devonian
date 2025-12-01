package com.github.synnerz.devonian.api.bufimgrenderer

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PostClientInit
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.opengl.GlTexture
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.TextureFormat
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.ResourceLocation
import org.lwjgl.opengl.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.PixelInterleavedSampleModel
import java.nio.ByteBuffer
import javax.imageio.ImageIO

class BufferedImageUploader(val name: String) : AbstractTexture() {
    var w: Int = 0
    var h: Int = 0
    var texId: Int = -1
    var pboId: Int = -1

    private val bimgFactory by lazy { BufferedImageFactoryImpl() }

    private fun create(img: BufferedImage) {
        w = img.width
        h = img.height
        texture = RenderSystem.getDevice().createTexture(name, 0, TextureFormat.RGBA8, w, h, 1, 1)
        textureView = RenderSystem.getDevice().createTextureView(texture!!, 0, 1)
        texId = (texture as GlTexture).glId()
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
            null as ByteBuffer?
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

    private fun uploadImpl(img: BufferedImage) {
        val w = img.width
        val h = img.height

        var img = img

        if (!isRGBAByteInterleaved(img)) {
            val newImg = bimgFactory.create(w, h)
            val g = newImg.createGraphics()
            g.drawImage(img, 0, 0, null)
            g.dispose()
            img = newImg
        }

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

    fun upload(img: BufferedImage) {
        if (RenderSystem.tryGetDevice() == null) EventBus.on<PostClientInit> { uploadImpl(img) }
        else uploadImpl(img)
    }

    fun register(mcid: ResourceLocation) = apply {
        val texMng = Devonian.minecraft?.textureManager
        if (texMng != null) texMng.register(mcid, this)
        else EventBus.on<PostClientInit> { event ->
            event.minecraft.textureManager.register(mcid, this)
        }
    }

    private fun destroy() {
        if (texId == -1) return
        GlStateManager._deleteTexture(texId)
        if (pboId != -1) GlStateManager._glDeleteBuffers(pboId)
        texture?.close()
        pboId = -1
        texId = pboId
    }

    companion object {
        private fun getImg(path: String): BufferedImage? {
            val stream = this::class.java.getResourceAsStream(path) ?: return null
            val img = ImageIO.read(stream)
            return img
        }

        fun fromResource(path: String) = getImg(path)?.let { img ->
            BufferedImageUploader(path).also {
                it.upload(img)
            }
        }

        private fun isRGBAByteInterleaved(img: BufferedImage): Boolean {
            val raster = img.raster

            val buf = raster.dataBuffer
            if (buf !is DataBufferByte) return false

            val sm = raster.sampleModel
            if (sm !is PixelInterleavedSampleModel) return false

            return sm.bandOffsets.withIndex().all { it.index == it.value }
        }
    }
}
