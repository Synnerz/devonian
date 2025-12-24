package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.splits.TimeUnit
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes

object RunSplits : TextHudFeature(
    "runSplits",
    "Displays how long your party has take to complete Blood Rush, Blood Open & Boss Enter",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
) {
    private val SETTING_SEND_ALL_END = addSwitch(
        "sendAllOnRunEnd",
        false,
        "Sends all of the splits in chat whenever the run ends",
        "Run Splits Send All End",
    )
    private val SETTING_FORMAT = addSelection(
        "format",
        0,
        listOf("Real Time", "Server Ticks", "Both"),
        "",
        "Time Format",
    )
    private val SETTING_SHOW_IN_BOSS = addSwitch(
        "showInBoss",
        false,
        "",
        "Show In Boss",
    )

    fun onFloorEnd() {
        if (!isEnabled()) return
        if (!SETTING_SEND_ALL_END.get()) return
        Scheduler.scheduleServerTask(2) {
            Stages.Clear.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()]).forEach {
                ChatUtils.sendMessage(it.replaceCodes())
            }
        }
    }

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            if (!isEditing) editTime = TimeUnit.EMPTY
            setLines(Stages.Clear.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()]))
            draw(event.ctx)
        }.setEnabled(Stages.Clear.isActiveState.zip(SETTING_SHOW_IN_BOSS.state, Boolean::or))
    }

    var editTime = TimeUnit.EMPTY
    override fun getEditText(): List<String> {
        if (editTime.isEmpty()) editTime = TimeUnit.now()
        return Stages.Clear.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()], editTime)
    }
}