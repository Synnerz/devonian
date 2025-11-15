package com.github.synnerz.devonian.api.bufimgrenderer

import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PostClientInit
import com.github.synnerz.devonian.mixin.accessor.DrawContextAccessor
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.platform.SourceFactor
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode
import kotlinx.atomicfu.atomic
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderPhase
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import net.minecraft.util.TriState
import java.awt.image.BufferedImage
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

abstract class BufferedImageRenderer<T>(val name: String, bilinear: TriState) {
    val uploader = BufferedImageUploader(name)
    val dirtyImage = atomic<BufferedImage?>(null)
    val bimgProvider: BufferedImageFactory = BufferedImageFactoryImpl()
    var lastFuture: Future<*>? = null
    val mcid = Identifier.of("devonian", "buffered_image/${name.lowercase()}")
    val layer = RenderLayer.of(
        "devonian/buffered_image_layer/${name.lowercase()}",
        1536,
        false,
        true,
        pipeline,
        RenderLayer.MultiPhaseParameters
            .builder()
            .texture(RenderPhase.Texture(mcid, bilinear, false))
            .build(false)
    )

    init {
        EventBus.on<PostClientInit> { event ->
            event.minecraft.textureManager.registerTexture(mcid, uploader)
        }
    }

    protected abstract fun drawImage(img: BufferedImage, param: T): BufferedImage

    fun update(w: Int, h: Int, param: T) {
        lastFuture?.cancel(false)
        lastFuture = pool.submit {
            val img = bimgProvider.create(w, h)
            dirtyImage.value = drawImage(img, param)
        }
    }

    private fun uploadImage() {
        val bimg = dirtyImage.getAndSet(null)
        if (bimg != null) uploader.upload(bimg)
    }

    fun draw(ctx: DrawContext, x: Float, y: Float, scale: Float = 1f) {
        uploadImage()
        draw(ctx, x, y, uploader.w * scale, uploader.h * scale)
    }

    fun drawStretched(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float) {
        uploadImage()
        draw(ctx, x, y, w, h)
    }

    private fun draw(ctx: DrawContext, x: Float, y: Float, w: Float, h: Float) {
        if (uploader.texId == -1) return

        val mat = ctx.matrices.peek().positionMatrix
        val consumer = (ctx as DrawContextAccessor).vertexConsumers
        val buf = consumer.getBuffer(layer)
        buf.vertex(mat, x, y, 0f).texture(0f, 0f).color(-1)
        buf.vertex(mat, x, y + h, 0f).texture(0f, 1f).color(-1)
        buf.vertex(mat, x + w, y, 0f).texture(1f, 0f).color(-1)
        buf.vertex(mat, x + w, y + h, 0f).texture(1f, 1f).color(-1)
    }

    companion object {
        val pool = ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, LinkedBlockingQueue())
        val pipeline = RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
            .withLocation("devonian/buffered_image_textured_triangle_strip")
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.TRIANGLE_STRIP)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(
                BlendFunction(
                    SourceFactor.SRC_ALPHA,
                    DestFactor.ONE_MINUS_SRC_ALPHA,
                    SourceFactor.ONE,
                    DestFactor.ZERO
                )
            )
            .build()
    }
}