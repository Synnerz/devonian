package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.api.splits.TimeUnit
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.SplitsFeature
import com.github.synnerz.devonian.utils.BasicState

object TriviaSplits : SplitsFeature(
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

    private val SETTING_SHOW_IN_BOSS = addSwitch(
        "showInBoss",
        false,
        "",
        "Show In Boss",
    )
    private var sent = false

    override fun getMaxName(): String = "Q3"

    override fun getSplits(style: TimeUnit.Style, force: TimeUnit?): List<String> =
        Stages.QuizSplits.getSplits(style, force)

    override fun getChatSplits(style: TimeUnit.Style, force: TimeUnit?): List<String> =
        listOf(super.getChatSplits(style, force).joinToString(" &f| "))

    override fun initialize() {
        super.initialize()

        on<ChatEvent> { event ->
            Stages.QuizSplits.onChat(event.message)
            if (event.matches("^\\[STATUE] Oruo the Omniscient: \\w+ answered the final question correctly!$".toRegex()) == null) return@on
            if (sent) return@on

            sendMessage()
            sent = true
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        sent = false
        Stages.QuizSplits.reset()
    }
}