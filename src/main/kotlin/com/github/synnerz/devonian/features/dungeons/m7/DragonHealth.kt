package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.PreExtractRenderEntityEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.ScoreboardEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

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

    private val nametagReg = "^﴾ [^\\sA-Za-z]* Withered Dragon ([\\d.,]+[KMB]?)/([\\d.,]+[KMB]?)❤ ﴿$".toRegex()
    private val scoreboardReg = "^- (\\w+) Dragon (.+)$".toRegex()

    private val healths = mutableListOf<Health>()
    private var dragons = mutableMapOf<M7Dragon, Int>()
    private var dragonHealths = ConcurrentHashMap<Int, String>()

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundAddEntityPacket ?: return@on
            if (packet.type != EntityType.ENDER_DRAGON) return@on

            val type = M7Dragon.entries.minBy {
                (it.path[0].x - packet.x) * (it.path[0].x - packet.x) +
                (it.path[0].y - packet.y) * (it.path[0].y - packet.y) +
                (it.path[0].z - packet.z) * (it.path[0].z - packet.z)
            }
            dragons[type] = packet.id
        }

        on<ScoreboardEvent> { event ->
            val match = event.matches(scoreboardReg) ?: return@on
            val type = match.getOrNull(0) ?: return@on
            val health = match.getOrNull(1) ?: return@on
            val dragon = M7Dragon.entries.find { it.displayName == type } ?: return@on
            val id = dragons[dragon] ?: return@on
            dragonHealths[id] = health.dropLast(1)
        }

        on<PreExtractRenderEntityEvent> { event ->
            val entity = event.entity as? EnderDragon ?: return@on

            if (entity.dragonDeathTime > 0) return@on

            val str = (minecraft.level?.getEntity(entity.id + 8) as? ArmorStand?)?.let { tag ->
                val name = tag.name.string
                val match = nametagReg.matchEntire(name) ?: return@let null
                val hp = match.groupValues.getOrNull(1) ?: return@let null
                // val hp = match.groupValues.getOrNull(1)?.let { StringUtils.parseShortenedNumber(it) } ?: return@on
                // val maxHp = match.groupValues.getOrNull(2)?.let { StringUtils.parseShortenedNumber(it) } ?: return@on
                return@let hp
            } ?: dragonHealths[entity.id] ?: return@on

            val pos = entity.getPosition(event.pt)
            healths.add(
                Health(
                    pos.x,
                    pos.y + 2.0,
                    pos.z,
                    str,
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