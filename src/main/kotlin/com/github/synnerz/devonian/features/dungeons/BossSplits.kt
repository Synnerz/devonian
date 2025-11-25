package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ScoreboardEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.api.splits.TimerSplit
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object BossSplits : TextHudFeature(
    "bossSplits",
    "Displays your current dungeon's boss splits, how long each section took to complete.",
    "Dungeons",
    "catacombs"
) {
    private val floorRegex = "^ +⏣ The Catacombs \\((\\w)(\\d+)\\)\$".toRegex()
    var currentSplit: TimerSplit? = null

    override fun initialize() {
        on<ScoreboardEvent> { event ->
            val match = event.matches(floorRegex) ?: return@on
            if (currentSplit != null) return@on
            val floorType = match[0]
            val floorNum = match[1].toInt()

            val currentFloor = if (floorNum == 7) "${floorType}${floorNum}" else "F${floorNum}"

            currentSplit = BossSplitTypes.byName(currentFloor)!!.ins
            currentSplit!!.event.register()
        }

        on<RenderOverlayEvent> { event ->
            val split = currentSplit ?: return@on

            if (split.children.first().time == 0L) return@on

            setLines(split.str())
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = BossSplitTypes.F6.ins.defaultStr()

    override fun onWorldChange(event: WorldChangeEvent) {
        val split = currentSplit ?: return
        split.reset()
        split.event.unregister()
        currentSplit = null
    }
}