package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageFactory
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageFactoryImpl
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.*
import com.github.synnerz.devonian.hud.texthud.TextRenderer
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import kotlin.math.ceil

object TextRendererImpl {
    private val bimgFactory: BufferedImageFactory = BufferedImageFactoryImpl()

    fun drawImage(img: BufferedImage, param: TextRenderer.RenderParams): BufferedImage {
        val rp = param.renderParams

        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.paint = Color(-1)

        val isDrop = rp.shadow == Shadow.Drop

        val tmpImg = if (isDrop) bimgFactory.create(img.width, img.height) else img
        val tmpG = if (isDrop) {
            val tmp = tmpImg.createGraphics()
            tmp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            tmp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            tmp
        } else g

        if (rp.backdrop == Backdrop.Full) {
            g.paint = Color(0, 0, 0, 64)
            g.fillRect(
                0,
                0,
                (param.visualWidth + 0.5f).toInt(),
                (param.lines.size * rp.fontSize + (param.lines.getOrNull(0)?.descent ?: 0f) + 0.5f).toInt()
            )
        }

        g.paint = Color(-1)
        param.lines.forEachIndexed { i, v ->
            val y = i * rp.fontSize + v.ascent
            val x = when (rp.align) {
                Align.Left -> 0f
                Align.Right -> param.visualWidth - v.visualWidth
                Align.Center
                    -> (param.visualWidth - v.visualWidth) * 0.5f
            }
            tmpG.transform = AffineTransform.getTranslateInstance(x.toDouble(), y.toDouble())

            if (rp.backdrop == Backdrop.Line) {
                g.paint = Color(0, 0, 0, 64)
                g.fillRect(
                    0,
                    v.ascent.toInt(),
                    ceil(v.visualWidth).toInt(),
                    ceil(rp.fontSize + v.descent).toInt()
                )
            }

            if (rp.shadow == Shadow.Outline) {
                val outlineColor = Color(0)
                tmpG.stroke = BasicStroke(rp.fontSize * 0.1f)
                v.shapes.forEach {
                    tmpG.paint = outlineColor
                    tmpG.draw(it.second)

                    tmpG.paint = it.first
                    tmpG.fill(it.second)
                }
            } else {
                v.shapes.forEach {
                    tmpG.paint = it.first
                    tmpG.fill(it.second)
                }
            }
        }

        if (isDrop) {
            val tmp2 = bimgFactory.create(img.width, img.height)
            dropShadowOp.filter(tmpImg, tmp2)
            val offset = rp.fontSize * 0.1
            g.drawImage(tmp2, AffineTransform.getTranslateInstance(offset, offset), null)
            g.drawImage(tmpImg, 0, 0, null)
            tmpG.dispose()
        }

        g.dispose()
        return img
    }

    private val dropShadowOp = RescaleOp(
        floatArrayOf(0.25f, 0.25f, 0.25f, 1f),
        floatArrayOf(0f, 0f, 0f, 0f),
        null
    )
}