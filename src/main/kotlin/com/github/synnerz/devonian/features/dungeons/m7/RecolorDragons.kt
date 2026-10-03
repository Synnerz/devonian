package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState

object RecolorDragons : Feature(
    "recolorDragons",
    "remove hurt color + color based on type",
    Categories.M7,
    "catacombs",
    searchTags = setOf("hurt"),
    subcategory = "Highlight",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.WitherKing.isActiveState)
    }

    val COLORS = M7Dragon.entries.map { (it.color.rgb and (0x00FFFFFF)) or (0xFF000000.toInt()) }.toIntArray()

    private var dragons = mutableMapOf<Int, Int>()

    override fun initialize() {
        on<M7Events.DragonSpawned> { event ->
            dragons[event.ent.id] = event.dragon.type.ordinal
        }

        on<M7Events.DragonDeath> { event ->
            dragons.remove(event.ent.id)
        }
    }

    fun getColorId(entityId: Int?): Int? {
        if (entityId == null) return null
        return dragons[entityId]
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        dragons.clear()
    }
}