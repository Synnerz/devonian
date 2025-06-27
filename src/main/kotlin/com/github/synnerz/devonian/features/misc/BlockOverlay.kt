package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.events.BlockOutlineEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.block.ShapeContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexRendering
import net.minecraft.util.Colors
import net.minecraft.world.EmptyBlockView

object BlockOverlay : Feature("blockOverlay") {
    override fun initialize() {
        on<BlockOutlineEvent> { event ->
            val blockPos = event.blockContext.blockPos()
            val context = event.renderContext
            val matrices = context.matrixStack() ?: return@on
            val consumers = context.consumers() ?: return@on
            val camera = minecraft.gameRenderer.camera
            val cam = camera.pos

            // accurate bounding box
            val blockShape = event.blockContext.blockState()
                .getOutlineShape(
                    EmptyBlockView.INSTANCE,
                    blockPos,
                    ShapeContext.of(camera.focusedEntity)
                )

            event.cancel()
            VertexRendering.drawOutline(
                matrices,
                consumers.getBuffer(RenderLayer.getLines()),
                blockShape,
                blockPos.x - cam.x,
                blockPos.y - cam.y,
                blockPos.z - cam.z,
                Colors.CYAN // TODO: make this customizable
            )
        }
    }
}