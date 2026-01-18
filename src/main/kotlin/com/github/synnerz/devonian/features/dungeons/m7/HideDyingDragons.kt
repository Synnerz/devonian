package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.PreExtractRenderEntityEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.world.entity.boss.enderdragon.EnderDragon

object HideDyingDragons : Feature(
    "hideDyingDragons",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "M7",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.WitherKing.isActiveState)
    }

    override fun initialize() {
        on<PreExtractRenderEntityEvent> { event ->
            if (event.entity !is EnderDragon) return@on
            if (event.entity.dragonDeathTime > 0) event.cancel()
        }
    }
}