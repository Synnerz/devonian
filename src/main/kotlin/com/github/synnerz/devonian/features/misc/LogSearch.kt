package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import com.github.synnerz.talium.components.*
import kotlinx.atomicfu.locks.withLock
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.Util
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    private val resultsChildren = mutableListOf<UIResult>()

    private data class UIResult(val result: LogSearcher.Result?, val ui: UIBase)
    private val RESULT_COMPARATOR = Comparator.comparing<UIResult, LocalDate>(
        { it.result?.log?.date },
        Comparator.nullsFirst<LocalDate> { t1, t2 -> t2.compareTo(t1) }
    )
        .thenByDescending { it.result?.log?.ordinal }
        .thenBy { it.result?.line ?: Int.MAX_VALUE }

    // me when
    private val regexRegex = "^/(.+?)/([a-z]*)$".toRegex()

    private var resultQ: ConcurrentLinkedQueue<LogSearcher.Result>? = null
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

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
                            it.isEmpty() || it.contains('i')
                        } ?: true
                    }
                }
                Devonian.minecraft.setScreenAndShow(this)
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
        results.yOffset = 0.0
        resultsChildren.clear()
        resultQ = null
    }

    fun search() {
        clearResults()
        LogSearcher.stop()
        if (input.text.isBlank()) {
            addMessage("§4Empty Search Criteria.")
            return
        }

        val filter = if (regex.value) {
            try {
                val reg = input.text.toRegex(if (caseI.value) setOf(RegexOption.IGNORE_CASE) else emptySet())
                RegexFilter(reg)
            } catch (e: Exception) {
                addMessage("§4Error creating Regex: $e")
                return
            }
        } else StringFilter(input.text, caseI.value)

        val q = ConcurrentLinkedQueue<LogSearcher.Result>()
        resultQ = q

        LogSearcher.submit(filter, q)
    }

    private fun createBaseChild(): UIRect {
        val child = UIRect(
            0.0, 0.0,
            100.0, 5.0,
        )

        child.onMouseEnter {
            child.setColor(100, 100, 100, 128)
        }
        child.onMouseLeave {
            child.setColor(0, 0, 0, 0)
        }

        return child
    }

    private fun insertChild(elem: UIResult) {
        var idx = resultsChildren.binarySearch(elem, RESULT_COMPARATOR)
        if (idx < 0) idx = idx.inv()

        elem.ui._y = idx * 5.0
        for (i in idx until resultsChildren.size) {
            resultsChildren[i].ui._y = (i + 1) * 5.0
        }

        resultsChildren.add(idx, elem)
        results.addChild(elem.ui)
    }

    private fun addMessage(msg: String) {
        val base = createBaseChild()

        base.addChild(
            UIText(
                0.0, 30.0,
                100.0, 70.0,
                text = msg,
            )
        )

        insertChild(UIResult(null, base))
    }

    private fun addResult(result: LogSearcher.Result) {
        val base = createBaseChild()

        val date =
            if (result.log === LogDate.Latest) "latest"
            else "${result.log.date.format(dateFormatter)}-${result.log.ordinal}"

        base.addChild(
            UIText(
                0.0, 30.0,
                20.0, 70.0,
                text = "§7$date §8${result.line}",
            )
        )
        base.addChild(
            UIText(
                20.0, 30.0,
                80.0, 70.0,
                text = result.match,
            )
        )
        base.onMouseClick {
            Util.getPlatform().openFile(result.path)
        }

        insertChild(UIResult(result, base))
    }

    override fun tick() {
        super.tick()

        val q = resultQ ?: return
        var l = q.size
        while (--l >= 0) {
            val e = q.poll() ?: break
            addResult(e)
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)
        background.draw()
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        background.handleKeyInput(keyEvent.key, keyEvent.scancode)
        return super.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        background.handleCharType(characterEvent.codepoint, characterEvent.codepointAsString(), -1)
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

data class LogDate(val date: LocalDate, val ordinal: Int) {
    companion object {
        val Latest = LogDate(LocalDate.now(), Int.MAX_VALUE)
    }
}

private object LogSearcher {
    private val logsFolder = File(Devonian.minecraft.gameDirectory, "logs")
    private val logRegex = "^(\\d{4})-(\\d{2})-(\\d{2})-(\\d+)\\.log\\.gz$".toRegex()

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
            pool = createPool()

            logsFolder.listFiles()?.forEach {
                if (it.isDirectory) return@forEach
                val date =
                    if (it.name == "latest.log") LogDate.Latest
                    else {
                        val m = logRegex.matchEntire(it.name) ?: return@forEach
                        val (year, month, day, ordinal) = m.groupValues.drop(1)
                        LogDate(LocalDate.of(year.toInt(), month.toInt(), day.toInt()), ordinal.toInt())
                    }
                pool!!.submit(Searcher(it, filter, resultQ, date))
            }

            pool!!.shutdown()
        } finally {
            lock.unlock()
        }
    }

    fun stop() {
        lock.withLock {
            pool?.shutdownNow()
            pool = null
        }
    }

    data class Result(val match: String, val line: Int, val path: File, val log: LogDate)

    private const val OFFSET1 = "[15:15:44] [Render thread/INFO] (Minecraft) [System] ".length
    private const val OFFSET2 = "[15:15:44] [Render thread/INFO] (Minecraft) [System] [CHAT] ".length
    class Searcher(val path: File, val filter: Filter, val q: ConcurrentLinkedQueue<Result>, val log: LogDate) : Runnable {
        override fun run() {
            val fileStream = FileInputStream(path)
            val inStream = if (path.extension == "gz") GZIPInputStream(fileStream) else fileStream
            val charStream = InputStreamReader(inStream)
            val bufStream = BufferedReader(charStream)

            bufStream.use {
                var str = it.readLine()
                var i = 1
                while (str != null && !Thread.interrupted()) {
                    if (str.startsWith("[CHAT]", OFFSET1)) {
                        val s = str.drop(OFFSET2).clearCodes()
                        if (filter.matches(s)) {
                            q.offer(Result(s, i, path, log))
                        }
                    }
                    str = it.readLine()
                    i++
                }
            }
        }
    }
}