package com.github.synnerz.devonian.features.debug.packetlogger

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.DebugLogger
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundPingPacket
import java.io.File
import java.util.*

object PacketLogger : TextHudFeature(
    "packetLogger",
    "",
    Categories.DEBUG,
    subcategory = "Packet Logger",
) {
    private val SETTING_START = addButton(
        ::startLogger,
        displayName = "Start Logger",
    )
    private val SETTING_STOP = addButton(
        ::stopLogger,
        displayName = "Stop Logger",
    )
    private val SETTING_FILTER = addTextInput(
        "filter",
        "",
        "",
        "Packet Filter",
    )

    private var lastFilter = ""
    private var filter = setOf<String>()

    private fun ensureFilter() {
        val str = SETTING_FILTER.get()
        if (str == lastFilter) return

        lastFilter = str
        filter = str.split(',').toSet()
    }

    private val packetLogger = DebugLogger("PacketLogger")

    private var lastTick = 0

    fun startLogger() {
        if (!isEnabled()) return
        if (packetLogger.startLogger()) ChatUtils.sendMessage("§aPacket Logger started")
        else ChatUtils.sendMessage("§4Packet Logger already active")
    }

    fun stopLogger() {
        if (!isEnabled()) return
        packetLogger.stopAndPrint()
    }

    private fun onPacket(packet: Packet<*>) {
        if (packetLogger.startTime == 0L) return
        if (packet is ClientboundPingPacket) {
            lastTick = packet.id
        }

        ensureFilter()
        val type = packet.type().flow.id()[0] + packet.type().id.path
        if (!filter.contains(type)) return

        val serializer = Registry.get(packet)

        val obj = JsonDataObject()
        obj.set("_class", packet.javaClass.name)
        obj.set("_type", type)
        obj.set("_tick", lastTick)
        obj.set("_time", System.currentTimeMillis() - packetLogger.startTime)

        serializer.serialize(packet, obj)

        packetLogger.offer(obj)
    }

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            onPacket(event.packet)
        }.setEnabled(packetLogger.loggerEnabled)
        on<PacketSentEvent> { event ->
            onPacket(event.packet)
        }.setEnabled(packetLogger.loggerEnabled)

        on<RenderOverlayEvent> { event ->
            if (packetLogger.startTime == 0L) return@on
            setLines(
                listOf(
                    "${packetLogger.logFile?.name}",
                    "Last Tick: $lastTick",
                    "Time: ${System.currentTimeMillis() - packetLogger.startTime}",
                )
            )
            draw(event.ctx)
        }.setEnabled(packetLogger.loggerEnabled)
    }

    override fun getEditText(): List<String> = listOf(
        "devonian-PacketLogger-1765612547398.log",
        "Last Tick: -69",
        "Time: 6942",
    )
}