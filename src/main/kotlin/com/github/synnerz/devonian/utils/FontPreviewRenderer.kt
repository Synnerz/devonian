package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageFactoryImpl
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.hud.texthud.StringParser
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud
import com.github.synnerz.devonian.hud.texthud.TextRenderer
import com.github.synnerz.devonian.utils.render.impl.TextRendererImpl
import kotlinx.atomicfu.atomic
import net.minecraft.client.gui.GuiGraphics
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.max

object FontPreviewRenderer : BufferedImageRenderer<FontPreviewRenderer.FontPreviewData>("fontPreviewRenderer") {
    data class FontPreviewData(val fonts: Map<Font, Pair<String, Float>>, val shadow: StylizedTextHud.Shadow)
    private data class FontLocation(
        val w: Float, val h: Float,
        val u0: Float, val v0: Float,
        val u1: Float, val v1: Float,
    )

    fun update(param: FontPreviewData) {
        super.update(1, 1, param)
    }

    private var currentRenderData = atomic<Map<String, FontLocation>?>(null)

    fun draw(ctx: GuiGraphics, font: Font, x: Float, y: Float) {
        val data = currentRenderData.value?.get(font.name) ?: return
        uploadImage()
        draw(ctx, x, y, data.w, data.h, data.u0, data.v0, data.u1, data.v1)
    }

    override fun drawImage(
        img: BufferedImage,
        param: FontPreviewData
    ): BufferedImage {
        val tmpG = img.createGraphics()
        tmpG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        tmpG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)

        val scale = Devonian.minecraft.window?.guiScale?.toFloat() ?: 1f

        val fontData = mutableMapOf<String, Pair<StringParser.LayoutLineData, Float>>()
        var padding = 0f
        var height = 0f
        var width = 0f
        param.fonts.forEach { (font, value) ->
            var (text, size) = value
            size *= scale

            val f = font.deriveFont(size)
            tmpG.font = f
            val line = StringParser.processString(
                text,
                tmpG,
                f, f, f,
                size
            )

            line.width += param.shadow.getSizeIncrease(size)
            height += line.ascent + line.descent + padding
            width = max(width, line.width)
            padding = 10f

            fontData[font.name] = line to size
        }

        tmpG.dispose()
        val factory = BufferedImageFactoryImpl()
        val img = factory.create(ceil(width).toInt(), ceil(height).toInt())
        val g = img.createGraphics()

        padding = 0f
        var y = 0f
        val renderData = mutableMapOf<String, FontLocation>()
        fontData.forEach { (font, value) ->
            val (line, size) = value

            val w = ceil(line.width).toInt()
            val h = ceil(line.ascent + line.descent).toInt()
            y += padding
            padding = 10f

            val textParams = StylizedTextHud.TextRenderParams(
                StylizedTextHud.Align.Left,
                param.shadow,
                StylizedTextHud.Backdrop.None,
                size,
            )
            val text = TextRendererImpl.drawImage(
                factory.create(w, h),
                TextRenderer.RenderParams(
                    textParams,
                    listOf(line),
                    line.width,
                    line.ascent,
                    line.descent,
                )
            )

            g.drawImage(text, 0, y.toInt(), null)
            renderData[font] = FontLocation(
                w / scale, h / scale,
                0f, y / height,
                w / width,
                (y + h) / height,
            )

            y += h
        }

        g.dispose()

        currentRenderData.value = renderData
        return img
    }
}