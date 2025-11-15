package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ScoreboardEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.api.splits.TimerSplit
import com.github.synnerz.devonian.features.WorldFeature
import com.github.synnerz.devonian.hud.HudManager

object BossSplits : WorldFeature("bossSplits", "catacombs") {
    private val floorRegex = "^ +⏣ The Catacombs \\((\\w)(\\d+)\\)\$".toRegex()
    private val hud = HudManager.createHud("BossSplits", BossSplitTypes.F6.ins.defaultStr())
    var currentSplit: TimerSplit? = null

    override fun initialize() {
        on<ScoreboardEvent> { event ->
            val match = event.matches(floorRegex) ?: return@on
            if (currentSplit != null) return@on
            val floorType = match[0]
            val floorNum = match[1].toInt()

            val currentFloor = if (floorNum == 7) "${floorType}${floorNum}" else "F${floorNum}"

            currentSplit = BossSplitTypes.byName(currentFloor)!!.ins
            currentSplit!!.register()
        }

        on<RenderOverlayEvent> { event ->
            if (currentSplit == null) return@on

            currentSplit?.draw(event.ctx, hud.x, hud.y, hud.scale)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        val split = currentSplit ?: return
        split.reset()
        split.unregister()
        currentSplit = null
    }
}