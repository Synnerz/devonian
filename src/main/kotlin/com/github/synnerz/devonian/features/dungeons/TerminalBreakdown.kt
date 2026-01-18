package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState

object TerminalBreakdown : Feature(
    "terminalBreakdown",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "F7",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F7.isActiveState)
    }

    private data class TermData(val name: String, var terms: Int = 0, var levers: Int = 0, var devs: Int = 0)
    private val dataComp = Comparator.comparingInt<TermData> { -it.terms }.thenBy { -it.devs }.thenBy { -it.levers }
    private val termRegex = "^(\\w{1,16}) (?:completed|activated) a (terminal|lever|device)! \\(\\d/[78]\\)$".toRegex()

    private val data = mutableMapOf<String, TermData>()

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.message == "The Core entrance is opening!") {
                ChatUtils.sendMessage("§fTerminals Breakdown:")
                Dungeons.players
                    .map { data.getOrElse(it.key) { TermData(it.key) } }
                    .sortedWith(dataComp)
                    .forEach { (name, terms, levers, devs) ->
                        ChatUtils.sendMessage("§b$name§f: Terminal x§a$terms§7 | §fLever x§a$levers§7 | §f Device x§a$devs")
                    }

                return@on
            }

            val matches = event.matches(termRegex) ?: return@on
            val ign = matches.getOrNull(0) ?: return@on
            val type = matches.getOrNull(1) ?: return@on

            val d = data.getOrPut(ign) { TermData(ign) }
            when (type) {
                "terminal" -> d.terms++
                "lever" -> d.levers++
                "device" -> d.devs++
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        data.clear()
    }
}