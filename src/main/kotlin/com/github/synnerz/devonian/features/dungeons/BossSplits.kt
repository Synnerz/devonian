package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.splits.TimeUnit
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.FixedWidthTextHud
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes

object BossSplits : TextHudFeature(
    "bossSplits",
    "Displays your current dungeon's boss splits, how long each section took to complete.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    searchTags = setOf("timer"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Boss.hasStartedState)
    }

    private val SETTING_SEND_ALL_END = addSwitch(
        "sendAllOnRunEnd",
        false,
        "Sends all of the splits in chat whenever the run ends.",
        "Boss Splits Send All End",
    )
    private val SETTING_FORMAT = addSelection(
        "format",
        0,
        listOf("Real Time", "Server Ticks", "Both"),
        "",
        "Time Format",
    )

    override fun createHud(): StylizedTextHud = object : FixedWidthTextHud(configName, this@BossSplits) {
        override fun getMaxLine(): String =
            if (SETTING_FORMAT.get() == 2) "Second Phase: 000.00s (000.00s)"
            else "Second Phase: 000.00s"
    }

    private fun getSplits(force: TimeUnit? = null): List<String> {
        return Stages.Boss.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()], force) +
            Stages.Root.getThisSplit(TimeUnit.Format.entries[SETTING_FORMAT.get()], force)
    }

    fun onFloorEnd() {
        if (!isEnabled()) return
        if (!SETTING_SEND_ALL_END.get()) return
        Scheduler.scheduleServerTask(2) {
            getSplits().forEach {
                ChatUtils.sendMessage(it.replaceCodes())
            }
        }
    }

    override fun initialize() {
        on<ClientThreadServerTickEvent> {
            setLines(getSplits())
        }.setEnabled(SETTING_FORMAT.state.map { it == 1 })

        on<RenderOverlayEvent> { event ->
            if (!isEditing) editTime = TimeUnit.EMPTY
            if (SETTING_FORMAT.get() != 1) setLines(getSplits())
            draw(event.ctx)
        }
    }

    var editTime = TimeUnit.EMPTY
    val floors = arrayOf(Stages.F1, Stages.F2, Stages.F3, Stages.F4, Stages.F5, Stages.F6, Stages.F7)
    override fun getEditText(): List<String> {
        if (editTime.isEmpty()) editTime = TimeUnit.now()

        val i = ((System.currentTimeMillis() / 1000L) % 7L).toInt()
        val old = Stages.BossFloor.chosenChild

        Stages.BossFloor.chosenChild = floors[i]
        val txt = getSplits(editTime)
        Stages.BossFloor.chosenChild = old

        return txt
    }
}