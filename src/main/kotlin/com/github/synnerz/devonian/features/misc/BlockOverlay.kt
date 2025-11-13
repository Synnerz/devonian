package com.github.synnerz.devonian.features.misc

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.api.events.BlockOutlineEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.ColorEnum
import com.github.synnerz.devonian.utils.JsonUtils
import net.minecraft.block.ShapeContext
import net.minecraft.world.EmptyBlockView
import java.awt.Color

object BlockOverlay : Feature("blockOverlay") {
    private const val SETTING_BOX_ENTITY = false;
    private var SETTING_WIRE_COLOR = Color.WHITE;
    private val SETTING_FILL_COLOR = Color(0)

    override fun initialize() {
        JsonUtils.set("blockOverlayColor", -1)

        DevonianCommand.command.subcommand("blockoverlay") { _, args ->
            if (args.isEmpty()) return@subcommand 0
            SETTING_WIRE_COLOR = ColorEnum.valueOf(args.first() as String).color
            JsonUtils.set("blockOverlayColor", SETTING_WIRE_COLOR.rgb)
            ChatUtils.sendMessage("&aSuccessfully set block overlay color to &6${args.first()}", true)
            1
        }.string("color").suggest("color", *ColorEnum.entries.map { it.name }.toTypedArray())

        JsonUtils.afterLoad {
            SETTING_WIRE_COLOR = Color(JsonUtils.get<Int>("blockOverlayColor") ?: -1, true)
        }

        on<BlockOutlineEvent> { event ->
            event.cancel()

            val entity = event.blockContext.entity()
            if (entity != null) {
                if (SETTING_BOX_ENTITY) {
                    val pos = entity.getLerpedPos(event.renderContext.tickCounter().getTickProgress(false))
                    Context.Immediate?.renderBox(
                        pos.x - entity.width * 0.5f,
                        pos.y,
                        pos.z - entity.width * 0.5,
                        entity.width.toDouble(),
                        entity.height.toDouble(),
                        SETTING_WIRE_COLOR,
                        phase = minecraft.options.perspective.isFirstPerson,
                        translate = true
                    )
                    Context.Immediate?.renderFilledBox(
                        pos.x - entity.width * 0.5f,
                        pos.y,
                        pos.z - entity.width * 0.5,
                        entity.width.toDouble(),
                        entity.height.toDouble(),
                        SETTING_WIRE_COLOR,
                        phase = minecraft.options.perspective.isFirstPerson,
                        translate = true
                    )
                }
                return@on
            }

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

            Context.Immediate?.renderBoxShape(
                blockShape,
                blockPos.x - cam.x,
                blockPos.y - cam.y,
                blockPos.z - cam.z,
                SETTING_WIRE_COLOR,
                minecraft.options.perspective.isFirstPerson
            )
            Context.Immediate?.renderFilledShape(
                blockShape,
                blockPos.x - cam.x,
                blockPos.y - cam.y,
                blockPos.z - cam.z,
                SETTING_FILL_COLOR,
                minecraft.options.perspective.isFirstPerson
            )
        }
    }
}