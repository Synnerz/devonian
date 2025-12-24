package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import kotlin.math.roundToInt

object CustomTerminalScale : Feature(
    "customTerminalScale",
    "Sets a different gui scale when you enter a terminal gui and re-sets it back to the original one once it's closed",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Terminals"
) {
    private val SETTING_TERMINAL_SCALE = addSlider(
        "terminalScale",
        0.0,
        0.0, 5.0,
        "0 = auto",
        "Terminal Gui Scale"
    )
    private val SETTING_TERMINAL_MELODY_SCALE = addSlider(
        "terminalMelodyScale",
        0.0,
        0.0, 5.0,
        "0 = auto",
        "Terminal Melody Gui Scale"
    )
    private val validGuis = listOf(
        "Click in order!".toRegex(),
        "^Select all the (.*?) items!$".toRegex(),
        "^What starts with: '(.*?)'\\?$".toRegex(),
        "^Change all to same color!$".toRegex(),
        "^Correct all the panes!$".toRegex()
    )
    private val melodyRegex = "^Click the button on time!$".toRegex()
    private var oldScale = -1

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet

            if (packet is ClientboundContainerClosePacket) {
                if (oldScale == -1) return@on

                minecraft.options.guiScale().set(oldScale)
                oldScale = -1
                return@on
            }

            if (packet !is ClientboundOpenScreenPacket) return@on
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on

            val title = packet.title.string
            val guiScale = minecraft.options.guiScale()

            if (validGuis.any { it.matches(title) }) {
                oldScale = guiScale.get()
                guiScale.set(SETTING_TERMINAL_SCALE.get().roundToInt())
                return@on
            }
            if (!melodyRegex.matches(title)) return@on

            oldScale = guiScale.get()
            guiScale.set(SETTING_TERMINAL_MELODY_SCALE.get().roundToInt())
        }

        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is ServerboundContainerClosePacket) return@on
            if (oldScale == -1) return@on

            minecraft.options.guiScale().set(oldScale)
            oldScale = -1
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        oldScale = -1
    }
}