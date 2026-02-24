package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.EntityDataEvent
import com.github.synnerz.devonian.api.events.PreExtractRenderEntityEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

object DragonHealth : Feature(
    "dragonHealth",
    "renders hp of m7 dragon below it",
    Categories.M7,
    "catacombs",
    subcategory = "Highlight",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.WitherKing.isActiveState)
    }

    private data class Health(val x: Double, val y: Double, val z: Double, val str: String)

    private val healths = mutableListOf<Health>()
    private var dragonHealths = ConcurrentHashMap<Int, String>()
    private var maxHp = 1_000_000_000

    override fun initialize() {
        on<EntityDataEvent> { event ->
            if (event.type != EntityType.ENDER_DRAGON) return@on

            val hp = event.data.find { it.id == 9 }?.value as? Float ?: return@on
            val hpI = hp.roundToInt()
            maxHp = max(maxHp, hpI)

            dragonHealths[event.entityId] = StringUtils.colorForNumber(hpI, maxHp) + StringUtils.shortenNumber(hpI)
        }

        on<PreExtractRenderEntityEvent> { event ->
            val entity = event.entity as? EnderDragon ?: return@on
            if (entity.dragonDeathTime > 0) return@on
            val hp = dragonHealths[entity.id] ?: return@on

            val pos = entity.getPosition(event.pt)
            healths.add(
                Health(
                    pos.x,
                    pos.y + 2.0,
                    pos.z,
                    hp,
                )
            )
        }

        on<RenderWorldEvent> {
            healths.forEach {
                Render3DImmediate.renderString(
                    it.str,
                    it.x, it.y, it.z,
                    8f,
                    maxDist = 100.0,
                    phase = true,
                )
            }
            healths.clear()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        maxHp = 1_000_000_000
    }
}