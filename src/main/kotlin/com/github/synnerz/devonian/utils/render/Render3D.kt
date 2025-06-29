package com.github.synnerz.devonian.utils.render

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.render.VertexRendering
import net.minecraft.util.shape.VoxelShape
import java.awt.Color

object Render3D {
    /**
     * - Draws a filled shape
     * @param ctx The WorldRenderContext
     * @param shape The VoxelShape to render
     * @param ox The x offset
     * @param oy The Y offset
     * @param oz The Z offset
     * @param color The color
     * @param phase Whether to render through walls or not (`false` = no)
     */
    fun renderFilled(
        ctx: WorldRenderContext,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        phase: Boolean = false
    ) {
        val consumers = ctx.consumers() ?: return
        val matrices = ctx.matrixStack() ?: return
        val layer = if (phase) DLayers.TRIANGLE_STRIP_ESP else DLayers.TRIANGLE_STRIP

        // TODO: make this more efficient later on
        //  (this does way too many calls but shouldn't matter much as of right now)
        shape.forEachBox { minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double ->
            VertexRendering.drawFilledBox(
                matrices,
                consumers.getBuffer(layer),
                minX + ox, minY + oy, minZ + oz,
                maxX + ox, maxY + oy, maxZ + oz,
                color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f
            )
        }
    }

    /**
     * - Draws a outline shape
     * @param ctx The WorldRenderContext
     * @param shape The VoxelShape to render
     * @param ox The x offset
     * @param oy The Y offset
     * @param oz The Z offset
     * @param color The color
     * @param phase Whether to render through walls or not (`false` = no)
     */
    fun renderOutline(
        ctx: WorldRenderContext,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        phase: Boolean = false
    ) {
        val consumers = ctx.consumers() ?: return
        val matrices = ctx.matrixStack() ?: return
        val layer = if (phase) DLayers.LINES_ESP else DLayers.LINES
        val argb = (color.alpha shl 24) or (color.red shl 16) or (color.green shl 8) or color.blue

        VertexRendering.drawOutline(
            matrices,
            consumers.getBuffer(layer),
            shape,
            ox, oy, oz,
            argb
        )
    }
}