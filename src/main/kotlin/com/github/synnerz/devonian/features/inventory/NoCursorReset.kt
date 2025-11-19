package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.features.Feature
import kotlinx.atomicfu.atomic
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket

object NoCursorReset : Feature(
    "noCursorReset",
    "Avoids resetting your cursor whenever navigating guis"
) {
    var closedGui = false
    var resetCursor = atomic(true)

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet

            when (packet) {
                is ClientboundContainerClosePacket -> {
                    closedGui = true
                }
                is ClientboundOpenScreenPacket -> {
                    if (closedGui) resetCursor.value = false
                }
                is ClientboundPingPacket -> {
                    if (closedGui) Scheduler.scheduleTask { resetCursor.value = true }
                    closedGui = false
                }
            }
        }
    }

    fun shouldReset(): Boolean {
        if (!isEnabled()) return true
        return resetCursor.value
    }
}