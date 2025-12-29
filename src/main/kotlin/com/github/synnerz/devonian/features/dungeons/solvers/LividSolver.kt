package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.Blocks
import java.awt.Color

object LividSolver : Feature(
    "lividSolver",
    "Highlights the correct livid in F5/M5",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F5.isActiveState)
    }

    private val SETTING_BOX_COLOR = addColorPicker(
        "boxColor",
        Color(0, 255, 255).rgb,
        "",
        "Livid Box Color",
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        3.0,
        1.0, 10.0,
        "The line width of the box",
        "Livid Line Width"
    )

    private val mapBlocks = mapOf(
        Blocks.WHITE_WOOL to "Vendetta",
        Blocks.MAGENTA_WOOL to "Crossed",
        Blocks.YELLOW_WOOL to "Arcade",
        Blocks.LIME_WOOL to "Smile",
        Blocks.GRAY_WOOL to "Doctor",
        Blocks.PURPLE_WOOL to "Purple",
        Blocks.GREEN_WOOL to "Frog",
        Blocks.BLUE_WOOL to "Scream",
        Blocks.RED_WOOL to "Hockey"
    )
    var started = false
    var currentLivid: String? = null
    var lividEnt: Entity? = null

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.message == "[BOSS] Livid: I respect you for making it to here, but I'll be your undoing.") started = true
        }
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundSectionBlocksUpdatePacket) return@on

            packet.runUpdates { t, u ->
                if (t.x != 5 || t.y != 108 || t.z != 43) return@runUpdates
                if (mapBlocks[u.block] != null) currentLivid = mapBlocks[u.block]
            }
        }

        on<TickEvent> {
            if (!started) return@on
            val name = if (currentLivid == null) return@on else "$currentLivid Livid"
            val world = minecraft.level ?: return@on
            lividEnt = world.players().find { it.name.string.contains(name) }
        }

        on<RenderWorldEvent> { event ->
            if (!started) return@on
            val entity = lividEnt ?: return@on
            val matrixStack = event.ctx.matrices()

            val cam = minecraft.gameRenderer.mainCamera.position.reverse()
            val width = entity.bbWidth.toDouble()
            val halfWidth = width / 2.0

            matrixStack.pushPose()
            matrixStack.translate(cam.x, cam.y, cam.z)

            Context.Immediate?.renderBox(
                entity.x - halfWidth, entity.y, entity.z - halfWidth,
                width, entity.bbHeight.toDouble(),
                SETTING_BOX_COLOR.getColor(),
                translate = false,
                lineWidth = SETTING_LINE_WIDTH.get(),
            )

            matrixStack.popPose()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        started = false
        currentLivid = "Hockey"
        lividEnt = null
    }
}