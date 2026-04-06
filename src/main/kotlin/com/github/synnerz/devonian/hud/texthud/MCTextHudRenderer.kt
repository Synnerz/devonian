package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.GuiTextRenderStateAccessor
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.*
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.Companion.BASE_FONT_SIZE
import com.github.synnerz.devonian.utils.FixedIdentityMap
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes
import com.github.synnerz.devonian.utils.render.states.QuadRenderState
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.GuiTextRenderState
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.PlainTextContents
import org.joml.Matrix3x2f
import java.util.*

class MCTextHudRenderer(name: String) : IStylizedTextHudRenderer(name) {
    override fun onUpdateLine(
        str: String,
        params: TextRenderParams
    ): LineData {
        val font = Devonian.minecraft.font
        val comp = Component.literal(str.replaceCodes())
        val w = font.width(comp) * parent.scale / parent.renderScale.toFloat()
        return CompLineData(
            w,
            parent.fontSize,
            parent.fontSize * 0.25f,
            false,
            comp,
        )
    }

    override fun renderText(ctx: GuiGraphicsExtractor) {
        val font = Devonian.minecraft.font
        val bounds = parent.getBounds()
        ctx.pose()
            .pushMatrix()
            .translate(bounds.x.toFloat(), bounds.y.toFloat())
            .scale(parent.scale)
        val mat = Matrix3x2f(ctx.pose())

        if (parent.backdrop == Backdrop.Full) ctx.guiRenderState.addGuiElement(
            QuadRenderState(
                RenderPipelines.GUI,
                mat,
                0f, 0f,
                bounds.w.toFloat() / parent.scale, bounds.h.toFloat() / parent.scale,
                0x40000000,
                ctx.scissorStack.peek()
            )
        )

        parent.lines.forEachIndexed { i, line ->
            val data = line.data as CompLineData

            val y = i * BASE_FONT_SIZE
            val x = when (parent.align) {
                Align.Left -> 0f
                Align.Right -> parent.lineWidth - data.width
                Align.Center -> (parent.lineWidth - data.width) * 0.5f
            } / parent.scale * parent.renderScale.toFloat() +
                if (parent.shadow == Shadow.Outline) 1f else 0f

            if (parent.backdrop == Backdrop.Line) ctx.guiRenderState.addGuiElement(
                QuadRenderState(
                    RenderPipelines.GUI,
                    mat,
                    x, y,
                    (x + data.width / parent.scale * parent.renderScale).toFloat(),
                    (y + (data.ascent + data.descent) / parent.scale * parent.renderScale).toFloat(),
                    0x40000000,
                    ctx.scissorStack.peek()
                )
            )

            if (parent.shadow == Shadow.Outline) {
                val black = cloneBlack(data.comp)
                ctx.guiRenderState.addText(
                    GuiTextRenderState(
                        font,
                        black.visualOrderText,
                        mat,
                        x.toInt() - 1, y.toInt() + 2,
                        0xFF000000.toInt(),
                        0,
                        false,
                        false,
                        ctx.scissorStack.peek()
                    ).also {
                        val that = it as? GuiTextRenderStateAccessor ?: return@also
                        that.`devonian$setXf`(x - 1)
                        that.`devonian$setYf`(y + 2)
                    }
                )
                ctx.guiRenderState.addText(
                    GuiTextRenderState(
                        font,
                        black.visualOrderText,
                        mat,
                        x.toInt() + 1, y.toInt() + 2,
                        0xFF000000.toInt(),
                        0,
                        false,
                        false,
                        ctx.scissorStack.peek()
                    ).also {
                        val that = it as? GuiTextRenderStateAccessor ?: return@also
                        that.`devonian$setXf`(x + 1)
                        that.`devonian$setYf`(y + 2)
                    }
                )
                ctx.guiRenderState.addText(
                    GuiTextRenderState(
                        font,
                        black.visualOrderText,
                        mat,
                        x.toInt(), y.toInt() + 2 - 1,
                        0xFF000000.toInt(),
                        0,
                        false,
                        false,
                        ctx.scissorStack.peek()
                    ).also {
                        val that = it as? GuiTextRenderStateAccessor ?: return@also
                        that.`devonian$setXf`(x)
                        that.`devonian$setYf`(y + 2 - 1)
                    }
                )
                ctx.guiRenderState.addText(
                    GuiTextRenderState(
                        font,
                        black.visualOrderText,
                        mat,
                        x.toInt(), y.toInt() + 2 + 1,
                        0xFF000000.toInt(),
                        0,
                        false,
                        false,
                        ctx.scissorStack.peek()
                    ).also {
                        val that = it as? GuiTextRenderStateAccessor ?: return@also
                        that.`devonian$setXf`(x)
                        that.`devonian$setYf`(y + 2 + 1)
                    }
                )
            }

            ctx.guiRenderState.addText(
                GuiTextRenderState(
                    font,
                    data.comp.visualOrderText,
                    mat,
                    x.toInt(), y.toInt() + 2,
                    -1,
                    // if (parent.backdrop == Backdrop.Line) 0x40000000 else 0,
                    0,
                    parent.shadow == Shadow.Drop,
                    false, // "includeEmpty" param i think this is right
                    ctx.scissorStack.peek()
                ).also {
                    val that = it as? GuiTextRenderStateAccessor ?: return@also
                    that.`devonian$setXf`(x)
                    that.`devonian$setYf`(y + 2)
                }
            )
        }

        ctx.pose().popMatrix()
    }

    class CompLineData(
        width: Float,
        ascent: Float,
        descent: Float,
        hasObfuText: Boolean,
        val comp: Component,
    ) : LineData(
        width,
        ascent,
        descent,
        hasObfuText,
    )

    companion object {
        private val cache = FixedIdentityMap<Component, Component>(128)

        private fun cloneBlack(c: Component): Component {
            return cache.getOrPut(c) {
                val comp = MutableComponent.create(
                    PlainTextContents.create(
                        buildString {
                            c.contents.visit {
                                append(it.clearCodes())
                                Optional.empty<Any>()
                            }
                        }
                    )
                )
                comp.withStyle(c.style.withColor(0))
                c.siblings.forEach { comp.append(cloneBlack(it)) }
                return@getOrPut comp
            }
        }
    }
}