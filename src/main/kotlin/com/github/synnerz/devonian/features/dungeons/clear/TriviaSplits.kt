package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.api.splits.TimeUnit
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object TriviaSplits : TextHudFeature(
    "triviaSplits",
    "Displays how long each question took",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    searchTags = setOf("quiz", "puzzle"),
) {
    // TODO: make actual "progress" hud
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState.zip(SETTING_SHOW_IN_BOSS.state, Boolean::or))
    }

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
    private var sent = false

    override fun initialize() {
        on<ChatEvent> { event ->
            Stages.QuizSplits.onChat(event.message)
            if (event.matches("^\\[STATUE] Oruo the Omniscient: \\w+ answered the final question correctly!$".toRegex()) == null) return@on
            if (sent) return@on

            ChatUtils.sendMessage(
                Stages.QuizSplits.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()]).joinToString(" &f| "),
                true
            )
            sent = true
        }

        on<RenderOverlayEvent> {
            setLines(Stages.QuizSplits.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()]))
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        sent = false
    }

    override fun getEditText(): List<String> = Stages.QuizSplits.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()], TimeUnit.now())
}