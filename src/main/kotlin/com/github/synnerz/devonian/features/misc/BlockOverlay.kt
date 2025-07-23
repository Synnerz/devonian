package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.events.BlockOutlineEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.ChatUtils
import com.github.synnerz.devonian.utils.ColorEnum
import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.render.Render3D
import net.minecraft.block.ShapeContext
import net.minecraft.world.EmptyBlockView
import java.awt.Color

object BlockOverlay : Feature("blockOverlay") {
    private var color: Color = Color.WHITE

    override fun initialize() {
        JsonUtils.set("blockOverlayColor", -1)

        DevonianCommand.command.subcommand("blockoverlay") { _, args ->
            if (args.isEmpty()) return@subcommand 0
            color = ColorEnum.valueOf(args.first() as String).color
            JsonUtils.set("blockOverlayColor", color.rgb)
            ChatUtils.sendMessage("&aSuccessfully set block overlay color to &6${args.first()}", true)
            1
        }.string("color").suggest("color", *ColorEnum.entries.map { it.name }.toTypedArray())

        JsonUtils.afterLoad {
            color = Color(JsonUtils.get<Int>("blockOverlayColor") ?: -1, true)
        }

        on<BlockOutlineEvent> { event ->
            val blockPos = event.blockContext.blockPos()
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
            Render3D.renderOutline(
                event.renderContext,
                blockShape,
                blockPos.x - cam.x,
                blockPos.y - cam.y,
                blockPos.z - cam.z,
                color,
                minecraft.options.perspective.isFirstPerson
            )
        }
    }
}