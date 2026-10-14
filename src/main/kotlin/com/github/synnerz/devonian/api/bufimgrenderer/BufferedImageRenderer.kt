package com.github.synnerz.devonian.api.bufimgrenderer

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.utils.render.states.TexturedQuadRenderState
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import kotlinx.atomicfu.atomic
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f
import java.awt.image.BufferedImage
import java.util.concurrent.Executors
import java.util.concurrent.Future

abstract class BufferedImageRenderer<T>(val name: String) {
    protected val uploader = BufferedImageUploader(name)
    protected val dirtyImage = atomic<Pair<BufferedImage, UpdateParam<T>>?>(null)
    protected val bimgProvider: BufferedImageFactory = BufferedImageFactory()
    protected var running = false
    protected var waiting: UpdateParam<T>? = null
    protected var lastFuture: Future<*>? = null
    protected val mcid = Identifier.fromNamespaceAndPath("devonian", "buffered_image/${name.lowercase()}")
    protected var valid = true
    protected var old = false
    var lastRenderParam: UpdateParam<T>? = null
        private set

    init {
        uploader.register(mcid)
    }

    data class UpdateParam<T>(val w: Int, val h: Int, val param: T)

    protected abstract fun drawImage(img: BufferedImage, param: T): BufferedImage

    protected open fun createImage(w: Int, h: Int): BufferedImage {
        return bimgProvider.createNative(w, h)
    }

    fun update(w: Int, h: Int, param: T) {
        if (running) {
            waiting = UpdateParam(w, h, param)
            return
        }
        running = true
        old = false
        lastFuture = pool.submit {
            try {
                val img = createImage(w, h)
                dirtyImage.value = drawImage(img, param) to UpdateParam(w, h, param)
            } catch (e: Exception) {
                println("error trying to render BufferedImage in $name")
                e.printStackTrace()
            }
            running = false
            val w = waiting ?: return@submit
            waiting = null
            update(w.w, w.h, w.param)
        }
    }

    fun uploadImage() {
        val draw = dirtyImage.getAndSet(null)
        if (draw != null && !old) {
            uploader.upload(draw.first)
            lastRenderParam = draw.second
            valid = true
        }
    }

    open fun invalidate() {
        valid = false
        old = true
        lastFuture?.cancel(true)
        lastRenderParam = null
    }

    fun draw(ctx: GuiGraphicsExtractor, x: Float, y: Float, scale: Float = 1f) {
        draw(ctx, x, y, uploader.w * scale, uploader.h * scale)
    }

    fun draw(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float) {
        drawImpl(ctx, x, y, w, h, 0f, 0f, 1f, 1f, -1)
    }

    fun draw(
        ctx: GuiGraphicsExtractor,
        x: Float, y: Float,
        u0: Float, v0: Float,
        u1: Float, v1: Float,
        color: Int = -1,
        scale: Float = 1f,
    ) {
        drawImpl(
            ctx,
            x, y,
            uploader.w * scale * (u1 - u0),
            uploader.h * scale * (v1 - v0),
            u0, v0,
            u1, v1,
            color,
        )
    }

    protected fun drawImpl(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, color: Int) {
        if (Devonian.minecraft.gui.hud.isHidden) return
        if (!uploader.hasImg) return
        if (!valid) return

        val textureView = uploader.textureView
        val sampler = uploader.sampler
        ctx.guiRenderState.addGuiElement(
            TexturedQuadRenderState(
                pipeline,
                TextureSetup(
                    textureView, null, null,
                    sampler, null, null,
                ),
                Matrix3x2f(ctx.pose()),
                x,
                y,
                x + w,
                y + h,
                u0, v0,
                u1, v1,
                color,
                ctx.scissorStack.peek()
            )
        )
    }

    fun dispose() {
        try {
            uploader.texture.close()
        } catch (_: IllegalStateException) {}
    }

    companion object {
        val pool = Executors.newVirtualThreadPerTaskExecutor()!!

        val pipeline = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation("devonian/buffered_image_textured_triangle_strip")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build()
    }
}