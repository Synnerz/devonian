package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.events.RenderOverlayEvent
import com.github.synnerz.devonian.events.TabUpdateEvent
import com.github.synnerz.devonian.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.render.Render2D

object PuzzlesDisplay : Feature("puzzlesDisplay", "catacombs") {
    private val puzzleStates = mutableListOf("✦", "✔", "✖")
    private val puzzleStatesColores = mutableListOf("&6✦", "&a✔", "&c✖")
    private val puzzlesRegex = "^ ([\\w ]+): \\[(✦|✔|✖)\\] ?\\(?(\\w+)?\\)?\$".toRegex()
    private val puzzlesCountRegex = "^Puzzles: \\((\\d+)\\)\$".toRegex()
    private val hud = HudManager.createHud("PuzzlesDisplay", "&d&lPuzzles&f: &65\n&d&lBoulder &6✦\n&d&lThree Weirdos &a✔")
    private var puzzlesCount = 0
    private var puzzles = mutableMapOf<String, Pair<Int, String>>()
    private var toDisplay = ""

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val countMatch = event.matches(puzzlesCountRegex)
            if (countMatch != null) {
                val ( amount ) = countMatch
                puzzlesCount = amount.toInt()
                toDisplay = "&d&lPuzzles&f: ${if (puzzlesCount > 3) "&6" else "&a"}$puzzlesCount\n"
                return@on
            }

            val matches = event.matches(puzzlesRegex) ?: return@on
            toDisplay = "&d&lPuzzles&f: ${if (puzzlesCount > 3) "&6" else "&a"}$puzzlesCount\n"
            val ( puzzleName, state, failedBy ) = matches

            puzzles[puzzleName] = Pair(puzzleStates.indexOf(state), failedBy)
            puzzles.entries.forEach {
                val name = it.key
                val ( entryState, entryFailedBy ) = it.value
                val failed = if (entryFailedBy.isEmpty()) "" else " &c$entryFailedBy"

                toDisplay += "&d&l$name ${puzzleStatesColores[entryState]}${failed}\n"
            }
        }

        on<WorldChangeEvent> {
            toDisplay = ""
            puzzlesCount = 0
            puzzles.clear()
        }

        on<RenderOverlayEvent> { event ->
            Render2D.drawStringNW(
                event.ctx,
                toDisplay,
                hud.x, hud.y, hud.scale
            )
        }
    }
}