package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.events.BlockOutlineEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.ChatUtils
import com.github.synnerz.devonian.utils.JsonUtils
import net.minecraft.block.ShapeContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexRendering
import net.minecraft.world.EmptyBlockView

object BlockOverlay : Feature("blockOverlay") {
    private val colors = mutableMapOf(
        "WHITE" to -1,
        "BLACK" to -16777216,
        "GRAY" to -8355712,
        "LIGHT_GRAY" to -6250336,
        "ALTERNATE_WHITE" to -4539718,
        "RED" to -65536,
        "LIGHT_RED" to -2142128,
        "GREEN" to -16711936,
        "BLUE" to -16776961,
        "YELLOW" to -256,
        "LIGHT_YELLOW" to -171,
        "PURPLE" to -11534256,
        "CYAN" to -11010079,
    )
    private var color: Int = -1

    override fun initialize() {
        JsonUtils.set("blockOverlayColor", -1)

        DevonianCommand.command.subcommand("blockoverlay") { _, args ->
            if (args.isEmpty()) return@subcommand 0
            color = colors[args.first()] ?: -1
            JsonUtils.set("blockOverlayColor", color)
            ChatUtils.sendMessage("&aSuccessfully set block overlay color to &6${args.first()}", true)
            1
        }.string("color").suggest("color",
            "WHITE",
            "BLACK",
            "GRAY",
            "LIGHT_GRAY",
            "ALTERNATE_WHITE",
            "RED",
            "LIGHT_RED",
            "GREEN",
            "BLUE",
            "YELLOW",
            "LIGHT_YELLOW",
            "PURPLE",
            "CYAN"
        )

        JsonUtils.afterLoad {
            color = JsonUtils.get<Int>("blockOverlayColor") ?: -1
        }

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
                color
            )
        }
    }
}