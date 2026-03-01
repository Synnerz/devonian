package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object PuzzlesDisplay : TextHudFeature(
    "puzzlesDisplay",
    "Displays the current Puzzle count as well as their name and state.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(
            Stages.Clear.hasFinishedState.map(Boolean::not),
            Dungeons.started.zip(SETTING_SHOW_CLEAR.state) { a, b -> !a || b }
        )
    }

    private val SETTING_USE_HYPIXEL_FORMAT = addSwitch(
        "useHypixelFormat",
        false,
        "Uses the hypixel tablist format instead of our custom one",
        "Hypixel Format"
    )
    private val SETTING_SHOW_CLEAR = addSwitch(
        "showClear",
        true,
        "Displays the puzzles even if the run has started",
        "Show In Clear"
    )
    private val puzzleStates = mutableListOf("✦", "✔", "✖")
    private val puzzleStatesColores = mutableListOf("&6✦", "&a✔", "&c✖")
    private val puzzlesRegex = "^ ([\\w ?]+): \\[(✦|✔|✖)] ?\\(?(\\w+)?\\)?$".toRegex()
    private val puzzlesCountRegex = "^Puzzles: \\((\\d+)\\)$".toRegex()
    private var puzzlesCount = 0
    private var puzzles = mutableMapOf<String, Pair<Int, String>>()
    private var puzzlesFormat = mutableListOf<String>()
    private var puzzleTitle = ""
    private var missingCount = 0
    private var unfoundPuzzles = 0

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val countMatch = event.matches(puzzlesCountRegex)
            if (countMatch != null) {
                val (amount) = countMatch
                puzzlesCount = amount.toInt()
                Scheduler.scheduleTask { puzzlesFormat.clear() }
                puzzleTitle = if (SETTING_USE_HYPIXEL_FORMAT.get()) "&l&bPuzzles: &f($puzzlesCount)"
                else "&d&lPuzzles&f: ${if (puzzlesCount > 3) "&6" else "&a"}$puzzlesCount"
                return@on
            }

            val matches = event.matches(puzzlesRegex) ?: return@on

            Scheduler.scheduleTask { puzzlesFormat.clear() }
            puzzleTitle = if (SETTING_USE_HYPIXEL_FORMAT.get()) "&l&bPuzzles: &f($puzzlesCount)"
            else "&d&lPuzzles&f: ${if (puzzlesCount > 3) "&6" else "&a"}$puzzlesCount"

            val (puzzleName, state, failedBy) = matches
            if (state == "✦") missingCount = (missingCount + 1).coerceAtMost(puzzlesCount)
            if (state == "✔") missingCount = (missingCount - 1).coerceAtLeast(0)
            if (puzzleName == "???") {
                unfoundPuzzles = (unfoundPuzzles + 1).coerceAtMost(puzzlesCount)
                return@on
            }

            val existed = puzzles.containsKey(puzzleName)

            puzzles[puzzleName] = Pair(puzzleStates.indexOf(state), failedBy)
            puzzles.entries.forEach {
                val name = it.key
                val (entryState, entryFailedBy) = it.value
                val failed = if (entryFailedBy.isEmpty()) "" else " &c$entryFailedBy"

                if (SETTING_USE_HYPIXEL_FORMAT.get())
                    Scheduler.scheduleTask { puzzlesFormat.add(" $name: &7[${puzzleStatesColores[entryState]}&7]${failed}") }
                else
                    Scheduler.scheduleTask { puzzlesFormat.add("&d&l$name ${puzzleStatesColores[entryState]}${failed}") }
            }

            if (!existed)
                unfoundPuzzles = (unfoundPuzzles - 1).coerceAtLeast(0)
        }

        on<TickEvent> {
            if (SETTING_USE_HYPIXEL_FORMAT.get() && unfoundPuzzles > 0 && puzzlesFormat.size < puzzlesCount) {
                for (idx in unfoundPuzzles downTo 1) {
                    puzzlesFormat.add(" &f???: &7[&6✦&7]")
                }
            }
            setLines(mutableListOf(puzzleTitle).apply { addAll(puzzlesFormat) })
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> =
        if (SETTING_USE_HYPIXEL_FORMAT.get()) listOf(
            "&l&bPuzzles: &f(5)",
            " &f???: &7[&6✦&7]",
            " &f???: &7[&6✦&7]",
            " &f???: &7[&6✦&7]",
            " &f???: &7[&6✦&7]",
            " &f???: &7[&6✦&7]",
        )
        else listOf(
            "&d&lPuzzles&f: &65",
            "&d&lBoulder &6✦",
            "&d&lThree Weirdos &a✔"
        )

    override fun onWorldChange(event: WorldChangeEvent) {
        puzzlesFormat.clear()
        puzzleTitle = ""
        puzzlesCount = 0
        puzzles.clear()
    }
}