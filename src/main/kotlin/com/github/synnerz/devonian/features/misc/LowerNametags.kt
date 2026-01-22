package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.PostExtractRenderEntityEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.math.min

object LowerNametags : Feature(
    "lowerNametags",
    "lower nametags of mobs",
    subcategory = "Tweaks",
    searchTags = setOf("giant", "health"),
) {
    private val cache = mutableMapOf<Int, Double>()
    private val grace = mutableMapOf<Int, Int>()
    private val unknown = mutableSetOf<Int>()

    override fun initialize() {
        on<PostExtractRenderEntityEvent> { event ->
            val state = event.state as? ArmorStandRenderState ?: return@on
            val stand = event.entity as? ArmorStand ?: return@on

            val offset = cache[stand.id]
            if (offset == null) {
                unknown.add(stand.id)
                return@on
            }
            state.y += offset
        }

        on<TickEvent> {
            val w = minecraft.level ?: return@on
            val arr = unknown.toList()
            unknown.clear()
            arr.forEach {
                val e = w.getEntity(it - 1)
                if (e == null) {
                    if ((grace.merge(it, 1, Int::plus) ?: 100) < 5) unknown.add(it)
                } else cache[it] = min(1.8 - e.bbHeight, 0.0)
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        cache.clear()
        grace.clear()
        unknown.clear()
    }
}