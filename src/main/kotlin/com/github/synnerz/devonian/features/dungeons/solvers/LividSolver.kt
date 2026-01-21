package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
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
    private val SETTING_TRACER = addSwitch(
        "tracer",
        true,
        "",
        "Livid Tracer",
    )
    private val SETTING_HIDE_WRONG_LIVID = addSwitch(
        "hideWrongLivid",
        false,
        "hides the wrong livid and only displays the right one",
        "Hide Wrong Livid"
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
    private val lividNameRegex = "^\\w+ Livid$".toRegex()
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

        on<PreExtractRenderEntityEvent> { event ->
            if (!SETTING_HIDE_WRONG_LIVID.get() || lividEnt == null) return@on
            val name = event.entity.name?.string ?: return@on
            if (!name.matches(lividNameRegex)) return@on
            if (event.entity.id == lividEnt?.id) return@on

            event.cancel()
        }

        on<RenderWorldEvent> {
            if (!started) return@on
            val entity = lividEnt ?: return@on

            val pos = entity.getPosition(minecraft.deltaTracker.getGameTimeDeltaPartialTick(false))

            Render3DImmediate.renderWireframeBox(
                pos.x, pos.y, pos.z,
                entity.bbWidth.toDouble(), entity.bbHeight.toDouble(),
                SETTING_BOX_COLOR.getColor(),
                lineWidth = SETTING_LINE_WIDTH.get(),
                centered = true,
            )
            if (SETTING_TRACER.get()) Render3DImmediate.renderTracer(
                pos.x, pos.y + 1.0, pos.z,
                SETTING_BOX_COLOR.getColor(),
            )
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        started = false
        currentLivid = "Hockey"
        lividEnt = null
    }
}