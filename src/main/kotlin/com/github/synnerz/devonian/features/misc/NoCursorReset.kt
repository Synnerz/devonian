package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.events.Events
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket

object NoCursorReset : Feature("noCursorReset") {
    var windowOpened: Long? = null
    var windowClosed: Long? = null

    override fun initialize() {
        Events.onPacketReceived { packet, _ ->
            when (packet) {
                is OpenScreenS2CPacket -> {
                    if (windowClosed == null) return@onPacketReceived

                    windowOpened = System.currentTimeMillis()
                }
                is CloseScreenS2CPacket -> {
                    windowOpened = null
                    windowClosed = System.currentTimeMillis()
                }
            }
        }
    }

    fun shouldReset(): Boolean {
        if (!isEnabled()) return true
        if (windowClosed == null || windowOpened == null) return true

        val state = windowOpened!! - windowClosed!! > 50
        if (!state) {
            windowOpened = null
            windowClosed = null
        }

        return state
    }
}