package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.utils.StringUtils.camelCaseToSentence
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

class DebugLogger(val name: String) {
    private var writer: OutputStreamWriter? = null
    private var ioThread: Thread? = null
    private var queue: Queue<JsonDataObject>? = null
    var startTime = 0L
        private set
    var logFile: File? = null
        private set
    val loggerEnabled = BasicState(false)

    fun startLogger(): Boolean {
        if (startTime != 0L) return false

        startTime = System.currentTimeMillis()

        val f = File(
            Devonian.minecraft.gameDirectory,
            "logs"
        ).resolve("devonian-$name-$startTime.log.gz")
        logFile = f
        val fileStream = FileOutputStream(f)
        val gzipStream = GZIPOutputStream(fileStream)
        val buffStream = BufferedOutputStream(gzipStream)
        val writer = OutputStreamWriter(buffStream, StandardCharsets.UTF_8)
        this.writer = writer

        val q = LinkedBlockingQueue<JsonDataObject>()
        queue = q
        ioThread = Thread({
            writer.write("[\n")
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val data = q.poll(100, TimeUnit.MILLISECONDS)
                    if (data != null) writer.write("$data,\n")
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }

            do {
                val data = q.poll() ?: break
                writer.write("$data,\n")
            } while (true)
            writer.write("]\n")
        }, "Devonian$name").also { it.start() }

        loggerEnabled.value = true
        return true
    }

    fun stopLogger(): File? {
        val file = logFile ?: return null

        loggerEnabled.value = false
        startTime = 0L
        logFile = null
        queue = null
        ioThread?.interrupt()
        ioThread?.join(5_000L)
        ioThread = null
        writer?.close()
        writer = null

        return file
    }

    fun stopAndPrint(): Boolean {
        val file = stopLogger()

        if (file == null) ChatUtils.sendMessage("§4${name.camelCaseToSentence()} not active")
        else ChatUtils.sendMessage(
            Component.literal("§a${name.camelCaseToSentence()} stopped")
                .withStyle(Style.EMPTY.withClickEvent(ClickEvent.OpenFile(file)))
        )

        return file != null
    }

    fun offer(obj: JsonDataObject) {
        queue?.offer(obj)
    }
}