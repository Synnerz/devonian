package com.github.synnerz.devonian.features.debug.packetlogger

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.google.gson.JsonObject
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundPingPacket
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream
import kotlin.math.log

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

    private var writer: OutputStreamWriter? = null
    private var ioThread: Thread? = null
    private var queue: Queue<JsonDataObject>? = null
    private var startTime = 0L
    private var lastTick = 0
    private val loggerEnabled = BasicState(false)

    fun startLogger() {
        if (startTime != 0L) {
            ChatUtils.sendMessage("§4Packet Logger already active")
            return
        }

        startTime = System.currentTimeMillis()

        val logFile = File(
            Devonian.minecraft.gameDirectory,
            "logs"
        ).resolve("devonian-packets-$startTime.log.gz")
        val fileStream = FileOutputStream(logFile)
        val gzipStream = GZIPOutputStream(fileStream)
        val buffStream = BufferedOutputStream(gzipStream)
        val writer = OutputStreamWriter(buffStream, StandardCharsets.UTF_8)
        this.writer = writer

        val q = LinkedBlockingQueue<JsonDataObject>()
        queue = q
        ioThread = Thread({
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val data = q.poll(100, TimeUnit.MILLISECONDS)
                    if (data != null) writer.write(data.toString() + '\n')
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }

            do {
                val data = q.poll() ?: break
                writer.write(data.toString() + '\n')
            } while (true)
        }, "DevonianPacketLogger").also { it.start() }

        loggerEnabled.value = true
        ChatUtils.sendMessage("§aPacket Logger started")
    }

    fun stopLogger() {
        if (startTime == 0L) {
            ChatUtils.sendMessage("§4Packet Logger not active")
            return
        }

        val logFile = File(
            Devonian.minecraft.gameDirectory,
            "logs"
        ).resolve("devonian-packets-$startTime.log.gz")

        loggerEnabled.value = false
        startTime = 0L
        ioThread?.interrupt()
        ioThread?.join(5_000L)
        ioThread = null
        writer?.close()
        writer = null

        ChatUtils.sendMessage(
            Component.literal("§aPacket Logger stopped")
                .withStyle(Style.EMPTY.withClickEvent(ClickEvent.OpenFile(logFile)))
        )
    }

    private fun onPacket(packet: Packet<*>) {
        if (startTime == 0L) return
        if (packet is ClientboundPingPacket) {
            lastTick = packet.id
        }

        ensureFilter()
        val type = packet.type().flow.id()[0] + packet.type().id.path
        if (!filter.contains(type)) return

        val serializer = Registry.get(packet)

        val obj = JsonDataObject(JsonObject())
        obj.set("_class", packet.javaClass.name)
        obj.set("_tick", lastTick)
        obj.set("_time", System.currentTimeMillis() - startTime)

        serializer.serialize(packet, obj)

        queue?.offer(obj)
    }

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            onPacket(event.packet)
        }.setEnabled(loggerEnabled)
        on<PacketSentEvent> { event ->
            onPacket(event.packet)
        }.setEnabled(loggerEnabled)

        on<RenderOverlayEvent> { event ->
            if (startTime == 0L) return@on
            setLines(
                listOf(
                    "devonian-packets-$startTime.log",
                    "Last Tick: $lastTick",
                    "Time: ${System.currentTimeMillis() - startTime}",
                )
            )
            draw(event.ctx)
        }.setEnabled(loggerEnabled)
    }

    override fun getEditText(): List<String> = listOf(
        "devonian-packets-1765612547398.log",
        "Last Tick: -69",
        "Time: 6942",
    )
}