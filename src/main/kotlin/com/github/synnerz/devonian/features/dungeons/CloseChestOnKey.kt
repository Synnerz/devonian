package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket

object CloseChestOnKey : Feature(
    "closeChestOnKey",
    "Closes a secret chest whenever you hit WASD (or rather moving) and shift keys",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
) {
    private val keybinds get() = listOf(
        minecraft.options.keyUp, // w
        minecraft.options.keyLeft, // a
        minecraft.options.keyRight, // d
        minecraft.options.keyDown, // s
        minecraft.options.keyShift, // shift
    )
    private var containerId = -1

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundOpenScreenPacket) return@on
            val containerName = packet.title.string ?: return@on
            if (containerName != "Chest") return@on

            Scheduler.scheduleTask {
                containerId = packet.containerId
            }
        }

        on<GuiKeyDownEvent> { event ->
            keybinds.forEach {
                if (containerId == -1) return@on
                if (!it.matches(event.event)) return@forEach

                event.cancel()
                minecraft.connection?.send(ServerboundContainerClosePacket(containerId))
                minecraft.setScreen(null)
                containerId = -1
                return@on
            }
        }
    }
}