package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.EntityType
import java.util.concurrent.ConcurrentHashMap

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

    private var dragons = ConcurrentHashMap<Int, Int>()

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundAddEntityPacket ?: return@on
            if (packet.type != EntityType.ENDER_DRAGON) return@on

            val type = M7Dragon.entries.minBy {
                (it.path[0].x - packet.x) * (it.path[0].x - packet.x) +
                (it.path[0].y - packet.y) * (it.path[0].y - packet.y) +
                (it.path[0].z - packet.z) * (it.path[0].z - packet.z)
            }
            dragons[packet.id] = type.ordinal
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