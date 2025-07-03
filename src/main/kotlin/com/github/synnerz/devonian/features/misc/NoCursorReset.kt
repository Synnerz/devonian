package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.events.PacketReceivedEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket

object NoCursorReset : Feature("noCursorReset") {
    var windowOpened: Long? = null
    var windowClosed: Long? = null

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet

            when (packet) {
                is OpenScreenS2CPacket -> {
                    if (windowClosed == null) return@on

                    windowOpened = System.currentTimeMillis()
                }
                is CloseScreenS2CPacket -> {
                    windowOpened = null
                    windowClosed = System.currentTimeMillis()
                }
            }
        }
    }

    @JvmOverloads
    fun shouldReset(y: Boolean = false): Boolean {
        if (!isEnabled()) return true
        if (windowClosed == null || windowOpened == null) return true

        val state = windowOpened!! - windowClosed!! > 100
        if (!state && y) {
            windowOpened = null
            windowClosed = null
        }

        return state
    }
}