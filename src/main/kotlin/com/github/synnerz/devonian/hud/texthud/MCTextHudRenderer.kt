package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.GuiTextRenderStateAccessor
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.*
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.Companion.BASE_FONT_SIZE
import com.github.synnerz.devonian.utils.FixedIdentityMap
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes
import com.github.synnerz.devonian.utils.render.states.QuadRenderState
import net.minecraft.client.gui.Font
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
            params.fontSize,
            params.fontSize * 0.25f,
            false,
            comp,
        )
    }

    private var params: TextRenderParams? = null
    private var metrics = TextRenderMetrics.EMPTY
    override fun onUpdateImage(params: TextRenderParams, metrics: TextRenderMetrics) {
        this.params = params
        this.metrics = metrics
    }

    override fun getRenderMetrics(): TextRenderMetrics = metrics

    override fun getRenderedLines(): Int = parent.lines.size

    private fun renderLineImpl(ctx: GuiGraphicsExtractor, mat: Matrix3x2f, font: Font, i: Int, line: Line, color: Int) {
        val params = this.params ?: return
        val data = line.data as CompLineData

        val y = i * BASE_FONT_SIZE
        val x = when (params.align) {
            Align.Left -> 0f
            Align.Right -> metrics.width - data.width
            Align.Center -> (metrics.width - data.width) * 0.5f
        } / parent.scale * parent.renderScale.toFloat() +
        if (params.shadow == Shadow.Outline) 1f else 0f

        if (params.backdrop == Backdrop.Line) ctx.guiRenderState.addGuiElement(
            QuadRenderState(
                RenderPipelines.GUI,
                mat,
                x, y,
                (x + data.width / parent.scale * parent.renderScale).toFloat(),
                (y + (data.ascent + data.descent) / parent.scale * parent.renderScale).toFloat(),
                (color ushr 26) shl 24,
                ctx.scissorStack.peek()
            )
        )

        if (params.shadow == Shadow.Outline) {
            val black = cloneBlack(data.comp)
            ctx.guiRenderState.addText(
                GuiTextRenderState(
                    font,
                    black.visualOrderText,
                    mat,
                    x.toInt() - 1, y.toInt() + 2,
                    color and 0xFF000000.toInt(),
                    0,
                    false,
                    false,
                    ctx.scissorStack.peek()
                ).also {
                    @Suppress("CAST_NEVER_SUCCEEDS")
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
                    color and 0xFF000000.toInt(),
                    0,
                    false,
                    false,
                    ctx.scissorStack.peek()
                ).also {
                    @Suppress("CAST_NEVER_SUCCEEDS")
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
                    color and 0xFF000000.toInt(),
                    0,
                    false,
                    false,
                    ctx.scissorStack.peek()
                ).also {
                    @Suppress("CAST_NEVER_SUCCEEDS")
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
                    color and 0xFF000000.toInt(),
                    0,
                    false,
                    false,
                    ctx.scissorStack.peek()
                ).also {
                    @Suppress("CAST_NEVER_SUCCEEDS")
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
                color,
                // if (params.backdrop == Backdrop.Line) 0x40000000 else 0,
                0,
                parent.shadow == Shadow.Drop,
                false, // "includeEmpty" param i think this is right
                ctx.scissorStack.peek()
            ).also {
                @Suppress("CAST_NEVER_SUCCEEDS")
                val that = it as? GuiTextRenderStateAccessor ?: return@also
                that.`devonian$setXf`(x)
                that.`devonian$setYf`(y + 2)
            }
        )
    }

    override fun renderText(ctx: GuiGraphicsExtractor) {
        val params = this.params ?: return

        val font = Devonian.minecraft.font
        val bounds = parent.getBounds()
        ctx.pose()
            .pushMatrix()
            .translate(bounds.x.toFloat(), bounds.y.toFloat())
            .scale(parent.scale)
        val mat = Matrix3x2f(ctx.pose())

        if (params.backdrop == Backdrop.Full) ctx.guiRenderState.addGuiElement(
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
            renderLineImpl(ctx, mat, font, i, line, -1)
        }

        ctx.pose().popMatrix()
    }

    override fun renderLine(ctx: GuiGraphicsExtractor, id: Int, pos: Int, color: Int) {
        val line = parent.lines.find { it.id == id } ?: return

        val font = Devonian.minecraft.font
        val bounds = parent.getBounds()
        ctx.pose()
            .pushMatrix()
            .translate(bounds.x.toFloat(), bounds.y.toFloat())
            .scale(parent.scale)
        val mat = Matrix3x2f(ctx.pose())

        renderLineImpl(ctx, mat, font, pos, line, color)

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

        private val replaceColorsRegex = "[§&][0-9a-fr]".toRegex(RegexOption.IGNORE_CASE)

        private fun cloneBlack(c: Component): Component {
            return cache.getOrPut(c) {
                val comp = MutableComponent.create(
                    PlainTextContents.create(
                        buildString {
                            c.contents.visit {
                                append(replaceColorsRegex.replace(it) {
                                    if (it.value.endsWith('r')) "§r§0"
                                    else "§0"
                                })
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