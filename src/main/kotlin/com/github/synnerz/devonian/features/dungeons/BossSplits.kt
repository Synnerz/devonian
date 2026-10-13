package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Stages
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
        return super.createRequirements() + listOf(Stages.Boss.hasStartedState)
    }

    override fun getMaxName(): String = "Second Phase"

    override fun getSplits(style: TimeUnit.Style, force: TimeUnit?): List<String> =
        Stages.Boss.getSplits(style, force) +
        Stages.Root.getThisSplit(style, force)

    override fun initialize() {
        super.initialize()

        Stages.Boss.feature = this
    }

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