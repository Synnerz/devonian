package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import com.github.synnerz.talium.components.*
import net.minecraft.Util
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPInputStream

object LogSearch : Screen(Component.literal("Devonian.LogSearch")) {
    private val background = UIRect(0.0, 0.0, 100.0, 100.0).setColor(0, 0, 0, 64)
    private val input = UITextInput(5.0, 5.0, 60.0, 7.0, "Search...", parent = background).also {
        it.setColor(0, 0, 0, 128)
    }
    private val regexTag = UIText(
        65.0, 0.0,
        7.0, 7.0,
        text = "Regex:", centered = true,
        parent = background
    ).also {
        it.textScale = 0.5f
    }
    private val regex = UICheckBox(65.0, 5.0, 7.0, 7.0, parent = background).also {
        it.setColor(0, 0, 0, 128)
        it.xAnimation = null
    }
    private val caseTag = UIText(
        72.0, 0.0,
        7.0, 7.0,
        text = "Ignore Case:", centered = true,
        parent = background
    ).also {
        it.textScale = 0.5f
    }
    private val caseI = UICheckBox(72.0, 5.0, 7.0, 7.0, value = true, parent = background).also {
        it.setColor(0, 0, 0, 128)
        it.xAnimation = null
    }
    private val searchButton = UIRect(80.0, 5.0, 15.0, 7.0, parent = background).also {
        it.setColor(0, 0, 0, 128)
        it.addChild(UIText(0.0, 0.0, 100.0, 100.0, "Search", true))
        it.onMouseRelease {
            search()
        }
    }
    private val results = UIScrollable(5.0, 15.0, 90.0, 85.0, parent = background).also {
        it.setColor(0, 0, 0, 128)
        it.drawScrollbar = true
    }

    // me when
    private val regexRegex = "^/(.+?)/([a-z]*)$".toRegex()

    private var resultQ: ConcurrentLinkedQueue<LogSearcher.Result>? = null

    fun initialize() {
        DevonianCommand.command.subcommand("search") { _, args ->
            val criteria = args.getOrNull(0)?.toString() ?: ""
            Scheduler.scheduleTask {
                reset()
                if (criteria.isNotEmpty()) {
                    val m = regexRegex.matchEntire(criteria)
                    if (m == null) input.text = criteria
                    else {
                        regex.value = true
                        input.text = m.groupValues.getOrElse(1) { "" }
                        caseI.value = m.groupValues.getOrNull(2)?.let {
                            if (it.isEmpty()) true
                            else it.contains('i')
                        } ?: true
                    }
                }
                Devonian.minecraft.setScreen(this)
            }
            return@subcommand 1
        }.greedyString("criteria")
    }

    fun reset() {
        clearResults()
        input.text = "Search..."
        regex.value = false
        caseI.value = true
    }

    fun clearResults() {
        results.clearChildren()
        resultQ = null
    }

    fun search() {
        clearResults()
        if (input.text.isBlank()) {
            addResult("§4Empty Search Criteria.")
            return
        }

        val filter = if (regex.value) {
            try {
                val reg = input.text.toRegex(if (caseI.value) setOf(RegexOption.IGNORE_CASE) else emptySet())
                RegexFilter(reg)
            } catch (e: Exception) {
                addResult("§4Error creating Regex: ${e.toString()}")
                return
            }
        } else StringFilter(input.text, caseI.value)

        val q = ConcurrentLinkedQueue<LogSearcher.Result>()
        resultQ = q

        LogSearcher.submit(filter, q)
    }

    fun addResult(str: String, path: File? = null) {
        results.addChild(
            UIRect(
                0.0, results.children.size * 5.0,
                100.0, 5.0,
            ).also { rect ->
                rect.onMouseEnter {
                    rect.setColor(100, 100, 100, 128)
                }
                rect.onMouseLeave {
                    rect.setColor(0, 0, 0, 0)
                }

                rect.addChild(
                    UIText(
                        0.0, 30.0,
                        100.0, 70.0,
                        text = str,
                    ).also { txt ->
                        if (path == null) return@also
                        txt.onMouseClick {
                            Util.getPlatform().openFile(path)
                        }
                    }
                )
            }
        )
    }

    override fun tick() {
        super.tick()

        val q = resultQ ?: return
        var l = q.size
        while (--l >= 0) {
            val e = q.poll() ?: break
            addResult("§8${e.line} §r${e.match}", e.path)
        }
    }

    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        super.render(guiGraphics, i, j, f)
        background.draw()
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        background.handleKeyInput(keyEvent.key, keyEvent.scancode)
        return super.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        background.handleCharType(characterEvent.codepoint, characterEvent.codepointAsString(), characterEvent.modifiers)
        return super.charTyped(characterEvent)
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun removed() {
        super.removed()
        clearResults()
    }
}

private interface Filter {
    fun matches(str: String): Boolean
}

private class StringFilter(val filter: String, val case: Boolean) : Filter {
    override fun matches(str: String): Boolean = str.contains(filter, case)
}

private class RegexFilter(val filter: Regex) : Filter {
    override fun matches(str: String): Boolean = filter.containsMatchIn(str)
}

private object LogSearcher {
    private val logsFolder = File(Devonian.minecraft.gameDirectory, "logs")
    private val logRegex = "^\\d{4}-\\d{2}-\\d{2}-\\d\\.log\\.gz$".toRegex()

    private var pool: AbstractExecutorService? = null
    private var lock = ReentrantLock(true)

    private fun createPool() = ThreadPoolExecutor(
        16, 16,
        0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(),
    ) { r -> Thread(r, "Devonian-LogSearcher") }

    fun submit(filter: Filter, resultQ: ConcurrentLinkedQueue<Result>) {
        if (!lock.tryLock()) return

        try {
            pool?.shutdownNow()
            pool = createPool()

            logsFolder.listFiles()?.forEach {
                if (it.isDirectory) return@forEach
                if (it.name != "latest.log" && !logRegex.matches(it.name)) return@forEach
                pool!!.submit(Searcher(it, filter, resultQ))
            }

            pool!!.close()
        } finally {
            lock.unlock()
        }
    }

    data class Result(val match: String, val line: Int, val path: File)

    private const val OFFSET1 = "[15:15:44] [Render thread/INFO] (Minecraft) [System] ".length
    private const val OFFSET2 = "[15:15:44] [Render thread/INFO] (Minecraft) [System] [CHAT] ".length
    class Searcher(val path: File, val filter: Filter, val q: ConcurrentLinkedQueue<Result>) : Runnable {
        override fun run() {
            val fileStream = FileInputStream(path)
            val inStream = if (path.extension == "gz") GZIPInputStream(fileStream) else fileStream
            val charStream = InputStreamReader(inStream)
            val bufStream = BufferedReader(charStream)

            bufStream.use {
                var str = it.readLine()
                var i = 1
                while (str != null) {
                    if (str.startsWith("[CHAT]", OFFSET1)) {
                        val s = str.drop(OFFSET2).clearCodes()
                        if (filter.matches(s)) {
                            q.offer(Result(s, i, path))
                        }
                    }
                    str = it.readLine()
                    i++
                }
            }
        }
    }
}