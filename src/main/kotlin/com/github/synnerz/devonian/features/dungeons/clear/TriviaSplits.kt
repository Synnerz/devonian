package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
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

    override fun initialize() {
        on<ChatEvent> { event ->
            Stages.QuizSplits.onChat(event.message)
        }

        on<RenderOverlayEvent> {
            setLines(Stages.QuizSplits.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()]))
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = Stages.QuizSplits.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()], TimeUnit.now())
}