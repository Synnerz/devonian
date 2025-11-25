package com.github.synnerz.devonian.api.bufimgrenderer

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.mixin.accessor.GuiGraphicsAccessor
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat.Mode
import kotlinx.atomicfu.atomic
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import java.awt.image.BufferedImage
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

abstract class BufferedImageRenderer<T>(val name: String) {
    protected val uploader = BufferedImageUploader(name)
    protected val dirtyImage = atomic<BufferedImage?>(null)
    protected val bimgProvider: BufferedImageFactory = BufferedImageFactoryImpl()
    protected var lastFuture: Future<*>? = null
    protected val mcid = ResourceLocation.fromNamespaceAndPath("devonian", "buffered_image/${name.lowercase()}")
    protected val layer = RenderType.create(
        "devonian/buffered_image_layer/${name.lowercase()}",
        1536,
        false,
        true,
        pipeline,
        RenderType.CompositeState
            .builder()
            .setTextureState(RenderStateShard.TextureStateShard(mcid, false))
            .createCompositeState(false)
    )
    protected var valid = true

    init {
        uploader.register(mcid)
    }

    protected abstract fun drawImage(img: BufferedImage, param: T): BufferedImage

    fun update(w: Int, h: Int, param: T) {
        lastFuture?.cancel(false)
        lastFuture = pool.submit {
            try {
                val img = bimgProvider.create(w, h)
                dirtyImage.value = drawImage(img, param)
            } catch (e: Exception) {
                println("error trying to render BufferedImage in $name")
                e.printStackTrace()
            }
        }
    }

    private fun uploadImage() {
        val bimg = dirtyImage.getAndSet(null)
        if (bimg != null) {
            uploader.upload(bimg)
            valid = true
        }
    }

    fun invalidate() {
        valid = false
    }

    fun draw(ctx: GuiGraphics, x: Float, y: Float, scale: Float = 1f) {
        draw(ctx, x, y, uploader.w * scale, uploader.h * scale)
        uploadImage()
    }

    fun drawStretched(ctx: GuiGraphics, x: Float, y: Float, w: Float, h: Float) {
        draw(ctx, x, y, w, h)
        uploadImage()
    }

    private fun draw(ctx: GuiGraphics, x: Float, y: Float, w: Float, h: Float) {
        if (Devonian.minecraft.options.hideGui) return
        if (uploader.texId == -1) return
        if (!valid) return

        val mat = ctx.pose()
        val consumer = (ctx as GuiGraphicsAccessor).vertexConsumers
        val buf = consumer.getBuffer(layer)
        buf.addVertexWith2DPose(mat, x, y, 0f).setUv(0f, 0f).setColor(-1)
        buf.addVertexWith2DPose(mat, x, y + h, 0f).setUv(0f, 1f).setColor(-1)
        buf.addVertexWith2DPose(mat, x + w, y, 0f).setUv(1f, 0f).setColor(-1)
        buf.addVertexWith2DPose(mat, x + w, y + h, 0f).setUv(1f, 1f).setColor(-1)
    }

    companion object {
        val pool = ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, LinkedBlockingQueue())
        val pipeline = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation("devonian/buffered_image_textured_triangle_strip")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, Mode.TRIANGLE_STRIP)
            .build()
    }
}