package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import net.minecraft.util.TriState
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.ceil

class TextRenderer(name: String) : BufferedImageRenderer<TextRenderer.TextRenderParams>(name, TriState.TRUE) {
    override fun drawImage(img: BufferedImage, param: TextRenderParams): BufferedImage {
        val g = img.createGraphics()
        g.font = param.font
        val ascent = g.fontMetrics.ascent
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.paint = Color(-1)

        if (param.backdrop == TextHudFeature.Backdrop.Full) {
            g.paint = Color(0, 0, 0, 64)
            g.fillRect(
                0,
                0,
                (param.lineVW.toInt() + 0.5f).toInt(),
                (param.lines.size * param.fontSize + 0.5f).toInt()
            )
        }

        param.lines.forEachIndexed { i, v ->
            val y = i * param.fontSize + ascent
            val x = when (param.align) {
                TextHudFeature.Align.Left -> 0f
                TextHudFeature.Align.Right -> param.lineVW - v.visualWidth
                TextHudFeature.Align.Center,
                TextHudFeature.Align.CenterIgnoreAnchor
                    -> (param.lineVW - v.visualWidth) * 0.5f
            }
            if (param.backdrop == TextHudFeature.Backdrop.Line) {
                g.paint = Color(0, 0, 0, 64)
                g.fillRect(
                    x.toInt(),
                    (y - ascent).toInt(),
                    ceil(v.visualWidth).toInt(),
                    ceil(param.fontSize).toInt()
                )
            }
            if (param.shadow) {
                g.paint = Color(63, 63, 63, 255)
                v.layoutShadow?.draw(g, x + param.fontSize * 0.1f, y + param.fontSize * 0.1f)
            }
            g.paint = Color(-1)
            v.layout.draw(g, x, y)
        }

        g.dispose()
        return img
    }

    data class TextRenderParams(
        val align: TextHudFeature.Align,
        val shadow: Boolean,
        val backdrop: TextHudFeature.Backdrop,
        val fontSize: Float,
        val font: Font,
        val lines: List<StringParser.LineData>,
        val lineVW: Float
    )
}