package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import java.util.concurrent.CopyOnWriteArrayList

object PuzzlesDisplay : TextHudFeature("puzzlesDisplay", "catacombs") {
    private val puzzleStates = mutableListOf("✦", "✔", "✖")
    private val puzzleStatesColores = mutableListOf("&6✦", "&a✔", "&c✖")
    private val puzzlesRegex = "^ ([\\w ]+): \\[(✦|✔|✖)\\] ?\\(?(\\w+)?\\)?\$".toRegex()
    private val puzzlesCountRegex = "^Puzzles: \\((\\d+)\\)\$".toRegex()
    private var puzzlesCount = 0
    private var puzzles = mutableMapOf<String, Pair<Int, String>>()
    private var toDisplay = CopyOnWriteArrayList<String>()

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val countMatch = event.matches(puzzlesCountRegex)
            if (countMatch != null) {
                val (amount) = countMatch
                puzzlesCount = amount.toInt()
                toDisplay.clear()
                toDisplay.add("&d&lPuzzles&f: ${if (puzzlesCount > 3) "&6" else "&a"}$puzzlesCount")
                return@on
            }

            val matches = event.matches(puzzlesRegex) ?: return@on
            toDisplay.clear()
            toDisplay.add("&d&lPuzzles&f: ${if (puzzlesCount > 3) "&6" else "&a"}$puzzlesCount")
            val (puzzleName, state, failedBy) = matches

            puzzles[puzzleName] = Pair(puzzleStates.indexOf(state), failedBy)
            puzzles.entries.forEach {
                val name = it.key
                val (entryState, entryFailedBy) = it.value
                val failed = if (entryFailedBy.isEmpty()) "" else " &c$entryFailedBy"

                toDisplay.add("&d&l$name ${puzzleStatesColores[entryState]}${failed}")
            }
        }

        on<WorldChangeEvent> {
            toDisplay.clear()
            puzzlesCount = 0
            puzzles.clear()
        }

        on<RenderOverlayEvent> { event ->
            setLines(toDisplay.toList())
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf(
        "&d&lPuzzles&f: &65",
        "&d&lBoulder &6✦",
        "&d&lThree Weirdos &a✔"
    )
}