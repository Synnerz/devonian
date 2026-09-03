package com.github.synnerz.devonian.api.bufimgrenderer

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PostClientInitEvent
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTexture
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.resources.Identifier
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class BufferedImageUploader(val name: String) : AbstractTexture() {
    var w: Int = 0
    var h: Int = 0
    var hasImg = false

    private val bimgFactory by lazy { BufferedImageFactory() }

    private fun create(img: NativeBufferedImage) {
        w = img.width
        h = img.height

        val device = RenderSystem.getDevice()
        texture = device.createTexture(
            name,
            GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
            GpuFormat.RGBA8_UNORM,
            w, h,
            1, 1,
        )
        sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
        textureView = device.createTextureView(texture!!, 0, 1)
    }

    private fun uploadImpl(img: BufferedImage) {
        val w = img.width
        val h = img.height

        var img = img

        if (img !is NativeBufferedImage) {
            val newImg = bimgFactory.createNative(w, h)
            val g = newImg.createGraphics()
            g.drawImage(img, 0, 0, null)
            g.dispose()
            img = newImg
        }

        if (texture == null) create(img)
        else if (w != this.w || h != this.h) {
            destroy()
            create(img)
        }

        hasImg = true
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture!!, img.backing)

        img.backing.close()
    }

    fun upload(img: BufferedImage) {
        if (RenderSystem.tryGetDevice() == null) EventBus.on<PostClientInitEvent> { uploadImpl(img) }
        else uploadImpl(img)
    }

    fun register(mcid: Identifier) = apply {
        // ignore intellij it lies
        val texMng: TextureManager? = Devonian.minecraft.textureManager
        if (texMng != null) texMng.register(mcid, this)
        else EventBus.on<PostClientInitEvent> { event ->
            event.minecraft.textureManager.register(mcid, this)
        }
    }

    private fun destroy() {
        hasImg = false
        super.close()
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
    }
}
