package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils
import java.util.LinkedList
import java.util.TreeSet
import kotlin.math.min

object TpsDisplay : TextHudFeature("tpsDisplay") {
    private val SETTING_SHOW_CUR = addSwitch(
        "showCurrent",
        true,
        "",
        "Show Current TPS",
    )
    private val SETTING_SHOW_MIN = addSwitch(
        "showMinimum",
        false,
        "",
        "Show Minimum TPS",
    )
    private val SETTING_SHOW_MAX = addSwitch(
        "showMaximum",
        false,
        "",
        "Show Maximum TPS",
    )
    private val SETTING_SHOW_AVG = addSwitch(
        "showAverage",
        false,
        "",
        "Show Average TPS",
    )

    private const val TTL = 5_000L

    private val arrTotal = LinkedList<Long>()
    private val arrSecond = LinkedList<Long>()
    private var since = System.currentTimeMillis()
    private data class Sample(val value: Int, val expiry: Long)
    private val minTree = TreeSet<Sample>(Comparator.comparingInt { it.value })
    private val maxTree = TreeSet<Sample>(Comparator.comparingInt { -it.value })

    private var lastCur = 0
    private var lastMin = 0
    private var lastMax = 0
    private var lastAvg = 0.0

    override fun initialize() {
        DevonianCommand.command.subcommand("tps") { _, _ ->
            val cur = StringUtils.colorForNumber(lastCur - 15, 5) + lastCur
            val avg = StringUtils.colorForNumber(lastAvg - 15, 5.0) + "%.1f".format(lastAvg)
            ChatUtils.sendMessage("Tps: $cur, Avg: $avg", withPrefix = true)
            1
        }

        on<ClientThreadServerTickEvent> {
            val t = System.currentTimeMillis()
            arrTotal.add(t + TTL)
            arrSecond.add(t + 1_000L)
        }

        on<TickEvent> {
            val t = System.currentTimeMillis()

            var iter = arrTotal.iterator()
            while (iter.hasNext() && iter.next() < t) iter.remove()
            iter = arrSecond.iterator()
            while (iter.hasNext() && iter.next() < t) iter.remove()

            val cur = arrSecond.size
            val dt = t - since
            val avg = if (dt == 0L) 0.0
                else arrTotal.size / (min(dt, TTL) / 50.0) * 20.0

            val ttl = if (dt < 1_000L) 1_000L - dt
                else TTL - 1_000L
            val s = Sample(cur, t + ttl)
            minTree.tailSet(s, true).clear()
            maxTree.tailSet(s, true).clear()
            minTree.add(s)
            maxTree.add(s)

            while (minTree.first().expiry < t) minTree.removeFirst()
            while (maxTree.first().expiry < t) maxTree.removeFirst()

            val min = minTree.first().value
            val max = maxTree.first().value

            setLines(format(cur, min, max, avg))
            lastCur = cur
            lastMin = min
            lastMax = max
            lastAvg = avg
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        arrTotal.clear()
        arrSecond.clear()
        minTree.clear()
        maxTree.clear()
        since = System.currentTimeMillis()
    }

    private fun format(cur: Int, min: Int, max: Int, avg: Double): List<String> {
        val curStr = StringUtils.colorForNumber(cur - 15, 5) + cur
        val minStr = StringUtils.colorForNumber(min - 15, 5) + min
        val maxStr = StringUtils.colorForNumber(max - 15, 5) + max
        val avgStr = StringUtils.colorForNumber(avg - 15, 5.0) + "%.1f".format(avg)

        val arr = mutableListOf<String>()
        when (
            (if (SETTING_SHOW_CUR.get()) 1 else 0) +
            (if (SETTING_SHOW_MIN.get()) 1 else 0) +
            (if (SETTING_SHOW_MAX.get()) 1 else 0) +
            (if (SETTING_SHOW_AVG.get()) 1 else 0)
        ) {
            0 -> {}
            1 -> {
                if (SETTING_SHOW_CUR.get()) arr.add("TPS: $curStr")
                if (SETTING_SHOW_MIN.get()) arr.add("TPS: $minStr")
                if (SETTING_SHOW_MAX.get()) arr.add("TPS: $maxStr")
                if (SETTING_SHOW_AVG.get()) arr.add("TPS: $avgStr")
            }

            else -> {
                if (SETTING_SHOW_CUR.get()) arr.add("Current TPS: $curStr")
                if (SETTING_SHOW_MIN.get()) arr.add("Minimum TPS: $minStr")
                if (SETTING_SHOW_MAX.get()) arr.add("Maximum TPS: $maxStr")
                if (SETTING_SHOW_AVG.get()) arr.add("Average TPS: $avgStr")
            }
        }

        return arr
    }

    override fun getEditText(): List<String> = format(20, 11, 21, 18.4)
}