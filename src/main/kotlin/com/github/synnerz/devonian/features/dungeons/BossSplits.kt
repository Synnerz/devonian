package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
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
    private val SETTING_FORMAT_TIME_HUMAN = addSwitch(
        "formatTimeInHuman",
        false,
        "Formats the splits' time into a more human readable time rather than just second (example: 02m 03s instead of 123s)",
        "Boss Splits Format Time Human",
    )
    private val SETTING_SEND_ALL_END = addSwitch(
        "sendAllOnRunEnd",
        false,
        "Sends all of the splits in chat whenever the run ends",
        "Boss Splits Send All End",
    )
    private val floorRegex = "^ +⏣ The Catacombs \\((\\w)(\\d+)\\)\$".toRegex()
    private val extraStatsRegex = "^ *> EXTRA STATS <\$".toRegex()
    var currentSplit: TimerSplit? = null

    override fun initialize() {
        on<ScoreboardEvent> { event ->
            val match = event.matches(floorRegex) ?: return@on
            if (currentSplit != null) return@on
            val floorType = match[0]
            val floorNum = match[1].toInt()

            val currentFloor = if (floorNum == 7) "${floorType}${floorNum}" else "F${floorNum}"

            currentSplit = BossSplitTypes.byName(currentFloor)!!.ins
        }

        on<ChatEvent> { event ->
            currentSplit?.let { split ->
                split.onChat(event, SETTING_FORMAT_TIME_HUMAN.get())
                event.matches(extraStatsRegex)?.let {
                    if (!SETTING_SEND_ALL_END.get()) return@let
                    Scheduler.scheduleServerTask(2) {
                        split.sendChat(SETTING_FORMAT_TIME_HUMAN.get())
                    }
                }
            }
        }

        on<RenderOverlayEvent> { event ->
            val split = currentSplit ?: return@on

            if (split.children.first().time == 0L) return@on

            setLines(split.str(SETTING_FORMAT_TIME_HUMAN.get()))
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = BossSplitTypes.F6.ins.defaultStr(SETTING_FORMAT_TIME_HUMAN.get())

    override fun onWorldChange(event: WorldChangeEvent) {
        val split = currentSplit ?: return
        split.reset()
        currentSplit = null
    }
}