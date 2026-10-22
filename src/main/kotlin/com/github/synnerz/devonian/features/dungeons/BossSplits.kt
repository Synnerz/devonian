package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.DungeonStages
import com.github.synnerz.devonian.api.splits.TimeUnit
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.SplitsFeature
import com.github.synnerz.devonian.utils.BasicState

object BossSplits : SplitsFeature(
    "bossSplits",
    "Displays your current dungeon's boss splits, how long each section took to complete.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    searchTags = setOf("timer"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(DungeonStages.Boss.hasStartedState)
    }

    override fun getMaxName(): String = "Second Phase"

    override fun getSplits(style: TimeUnit.Style, force: TimeUnit?): List<String> =
        DungeonStages.Boss.getSplits(style, force) +
        DungeonStages.Root.getThisSplit(style, force)

    override fun initialize() {
        super.initialize()

        DungeonStages.Boss.feature = this
    }

    val floors = arrayOf(DungeonStages.F1, DungeonStages.F2, DungeonStages.F3, DungeonStages.F4, DungeonStages.F5, DungeonStages.F6, DungeonStages.F7)
    override fun getEditText(): List<String> {
        if (editTime.isEmpty()) editTime = TimeUnit.now()

        val i = ((System.currentTimeMillis() / 1000L) % 7L).toInt()
        val old = DungeonStages.BossFloor.chosenChild

        DungeonStages.BossFloor.chosenChild = floors[i]
        val txt = getSplits(editTime)
        DungeonStages.BossFloor.chosenChild = old

        return txt
    }
}