package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.PreExtractRenderEntityEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.decoration.ArmorStand

object DragonHealth : Feature(
    "dragonHealth",
    "renders hp of m7 dragon below it",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "M7",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.WitherKing.isActiveState)
    }

    private data class Health(val x: Double, val y: Double, val z: Double, val str: String)

    private val healths = mutableListOf<Health>()
    private val nametagReg = "^﴾ [^\\sA-Za-z]* Withered Dragon ([\\d.,]+[KMB]?)/([\\d.,]+[KMB]?)❤ ﴿$".toRegex()

    override fun initialize() {
        on<PreExtractRenderEntityEvent> { event ->
            val entity = event.entity as? EnderDragon ?: return@on

            if (entity.dragonDeathTime > 0) return@on

            val tag = minecraft.level?.getEntity(entity.id + 8) as? ArmorStand? ?: return@on
            val name = tag.name.string
            val match = nametagReg.matchEntire(name) ?: return@on
            val hp = match.groupValues.getOrNull(1) ?: return@on
            // val hp = match.groupValues.getOrNull(1)?.let { StringUtils.parseShortenedNumber(it) } ?: return@on
            // val maxHp = match.groupValues.getOrNull(2)?.let { StringUtils.parseShortenedNumber(it) } ?: return@on

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
}