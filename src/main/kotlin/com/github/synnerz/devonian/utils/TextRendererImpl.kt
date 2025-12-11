package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.Align
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.Backdrop
import com.github.synnerz.devonian.hud.texthud.TextRenderer
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.ceil

object TextRendererImpl {
    fun drawImage(img: BufferedImage, param: TextRenderer.RenderParams): BufferedImage {
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.paint = Color(-1)

        if (param.renderParams.backdrop == Backdrop.Full) {
            g.paint = Color(0, 0, 0, 64)
            g.fillRect(
                0,
                0,
                (param.visualWidth + 0.5f).toInt(),
                (param.lines.size * param.renderParams.fontSize + (param.lines.getOrNull(0)?.descent ?: 0f) + 0.5f).toInt()
            )
        }

        param.lines.forEachIndexed { i, v ->
            val y = i * param.renderParams.fontSize + v.ascent
            val x = when (param.renderParams.align) {
                Align.Left -> 0f
                Align.Right -> param.visualWidth - v.visualWidth
                Align.Center
                    -> (param.visualWidth - v.visualWidth) * 0.5f
            }
            if (param.renderParams.backdrop == Backdrop.Line) {
                g.paint = Color(0, 0, 0, 64)
                g.fillRect(
                    x.toInt(),
                    (y - v.ascent).toInt(),
                    ceil(v.visualWidth).toInt(),
                    ceil(param.renderParams.fontSize + v.descent).toInt()
                )
            }
            if (param.renderParams.shadow) {
                g.paint = Color(63, 63, 63, 255)
                v.layoutShadow?.draw(g, x + param.renderParams.fontSize * 0.1f, y + param.renderParams.fontSize * 0.1f)
            }
            g.paint = Color(-1)
            v.layout.draw(g, x, y)
        }

        g.dispose()
        return img
    }
}