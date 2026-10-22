package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.DungeonStages
import com.github.synnerz.devonian.api.splits.TimeUnit
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.SplitsFeature
import com.github.synnerz.devonian.utils.BasicState

object RunSplits : SplitsFeature(
    "runSplits",
    "Displays how long your party has take to complete Blood Rush, Blood Open & Boss Enter.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    searchTags = setOf("dungeons", "timer"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(DungeonStages.Clear.isActiveState.zip(SETTING_SHOW_IN_BOSS.state, Boolean::or))
    }
    private val SETTING_SHOW_IN_BOSS = addSwitch(
        "showInBoss",
        false,
        "",
        "Show In Boss",
    )

    override fun getMaxName(): String = "Portal Enter"

    override fun getSplits(style: TimeUnit.Style, force: TimeUnit?): List<String> =
        DungeonStages.Clear.getSplits(style, force)

    override fun initialize() {
        super.initialize()

        DungeonStages.Clear.feature = this
    }
}