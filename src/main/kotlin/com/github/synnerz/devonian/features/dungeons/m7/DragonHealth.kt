package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ExtractRenderEntityEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.world.entity.boss.enderdragon.EnderDragon

object DragonHealth : Feature(
    "dragonHealth",
    "renders hp of m7 dragon below it",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "World",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.WitherKing.isActiveState)
    }

    override fun initialize() {
        on<ExtractRenderEntityEvent> { event ->
            val entity = event.entity as? EnderDragon ?: return@on

            val hp = entity.health
            val max = entity.maxHealth
            if (hp <= 0f) return@on
            Context.Immediate?.renderString(
                StringUtils.colorForNumber(hp.toDouble(), max.toDouble()) + StringUtils.shortenNumber(hp.toInt()),
                entity.x, entity.y + 2.0, entity.z,
                8f,
                backgroundBox = false,
                increase = false,
                phase = true,
            )
        }
    }
}