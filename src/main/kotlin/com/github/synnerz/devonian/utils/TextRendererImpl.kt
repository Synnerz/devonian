package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageFactory
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageFactoryImpl
import com.github.synnerz.devonian.hud.texthud.BImgTextHudRenderer
import com.github.synnerz.devonian.hud.texthud.StringParser
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.*
import com.github.synnerz.devonian.hud.texthud.TextRenderer
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints.*
import java.awt.Shape
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import kotlin.math.ceil

object TextRendererImpl {
    private val bimgFactory: BufferedImageFactory = BufferedImageFactoryImpl()

    fun drawImage(img: BufferedImage, param: TextRenderer.RenderParams): BufferedImage {
        val rp = param.renderParams

        val g = img.createGraphics()
        val aa = if (!param.noAAHint || BImgTextHudRenderer.fontMainName != "Mojangles") VALUE_ANTIALIAS_ON else VALUE_ANTIALIAS_OFF
        g.setRenderingHint(KEY_ANTIALIASING, aa)
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON)
        if (rp.shadow == Shadow.Outline) g.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE)
        g.paint = Color(-1)

        val isDrop = rp.shadow == Shadow.Drop

        val tmpImg = if (isDrop) bimgFactory.create(img.width, img.height) else img
        val tmpG = if (isDrop) {
            val tmp = tmpImg.createGraphics()
            tmp.setRenderingHint(KEY_ANTIALIASING, aa)
            tmp.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON)
            tmp
        } else g

        if (rp.backdrop == Backdrop.Full) {
            g.paint = Color(0, 0, 0, 64)
            g.fillRect(
                0,
                0,
                (param.width + 0.5f).toInt(),
                (param.lines.size * rp.fontSize + (param.lines.getOrNull(0)?.descent ?: 0f) + 0.5f).toInt()
            )
        }

        g.paint = Color(-1)
        val empty = AffineTransform.getTranslateInstance(0.0, 0.0)
        param.lines.forEachIndexed { i, line ->
            val y = i * rp.fontSize
            val x = when (rp.align) {
                Align.Left -> 0f
                Align.Right -> param.width - line.width
                Align.Center
                    -> (param.width - line.width) * 0.5f
            }

            if (rp.backdrop == Backdrop.Line) {
                g.paint = Color(0, 0, 0, 64)
                g.fillRect(
                    x.toInt(),
                    (y + line.ascent - rp.fontSize).toInt(),
                    ceil(line.width).toInt(),
                    ceil(rp.fontSize + line.descent).toInt()
                )
            }

            tmpG.transform = AffineTransform.getTranslateInstance(x.toDouble(), y.toDouble() + line.ascent)
            if (line.shapes == null) {
                val shapes = mutableListOf<Pair<Color, Shape>>()
                var x = 0.0
                val frc = g.fontRenderContext
                line.atts.forEach { v ->
                    if (!StringParser.isColorCode(v.t)) return@forEach
                    if (v.s >= v.e) return@forEach

                    val tyl = TextLayout(line.attStr.getIterator(null, v.s, v.e), frc)
                    val shape = tyl.getOutline(AffineTransform.getTranslateInstance(x, 0.0))
                    x += tyl.advance
                    shapes.add(Pair(StringParser.COLORS[v.t] ?: Color(-1), shape))
                }
                line.shapes = shapes
            }
            if (rp.shadow == Shadow.Outline) {
                tmpG.translate(rp.fontSize * 0.1, 0.0)
                val outlineColor = Color(0)
                tmpG.stroke = BasicStroke(((rp.fontSize * 0.1f).toInt() or 1).toFloat())
                line.shapes!!.forEach {
                    tmpG.paint = outlineColor
                    tmpG.draw(it.second)

                    tmpG.paint = it.first
                    tmpG.fill(it.second)
                }
            } else {
                line.shapes!!.forEach {
                    tmpG.paint = it.first
                    tmpG.fill(it.second)
                }
            }
            tmpG.transform = empty
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