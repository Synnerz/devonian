package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.EntityEquipmentEvent
import com.github.synnerz.devonian.api.events.NameChangeEvent
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs

object HideHealerOrbs : Feature(
    "hideHealerOrbs",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Hiders",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Root.isActiveState)
    }

    private data class Key(val x: Int, val z: Int)
    private class Entry(val x: Double, val y: Double, val z: Double) {
        fun matches(x: Double, y: Double, z: Double): Boolean {
            return abs(x - this.x) < 8.0 &&
                abs(y - this.y) < 8.0 &&
                abs(z - this.z) < 8.0
        }
    }
    private val grid = linkedMapOf<Key, MutableList<Entry>>()

    private fun add(x: Double, y: Double, z: Double) {
        val key = Key(x.toInt() shr 3, z.toInt() shr 3)
        val entry = Entry(x, y, z)
        grid.getOrPut(key) { mutableListOf() }.add(entry)
        var dx = x % 8.0
        var dz = z % 8.0
        if (dx < 0.0) dx += 8.0
        if (dz < 0.0) dz += 8.0
        val ox = if (dx < 4.0) -1 else 1
        val oz = if (dz < 4.0) -1 else 1
        grid.getOrPut(Key(key.x + ox, key.z)) { mutableListOf() }.add(entry)
        grid.getOrPut(Key(key.x, key.z + oz)) { mutableListOf() }.add(entry)
        grid.getOrPut(Key(key.x + ox, key.z + oz)) { mutableListOf() }.add(entry)
    }
    private fun get(x: Double, z: Double): List<Entry>? {
        val key = Key(x.toInt() shr 3, z.toInt() shr 3)
        return grid[key]
    }
    private val addQueue = ConcurrentLinkedQueue<Triple<Double, Double, Double>>()

    private val orbIds = setOf(
        "DUNGEON_BLUE_SUPPORT_ORB",
        "DUNGEON_RED_SUPPORT_ORB",
        "DUNGEON_GREEN_SUPPORT_ORB",
    )
    private val orbNames = listOf(
        "ABILITY DAMAGE",
        "DAMAGE",
        "DEFENSE",
    )

    override fun initialize() {
        on<NameChangeEvent> { event ->
            if (event.type != EntityType.ARMOR_STAND) return@on

            if (!orbNames.any { event.name.startsWith(it) }) return@on

            Scheduler.scheduleTask {
                minecraft.level?.removeEntity(event.entityId, Entity.RemovalReason.DISCARDED)
            }
        }

        on<EntityEquipmentEvent> { event ->
            if (event.type != EntityType.ARMOR_STAND) return@on

            event.slots.forEach { (slot, item) ->
                if (slot != EquipmentSlot.HEAD) return@forEach

                val item = item ?: return@on
                if (item.isEmpty) return@on

                val data = ItemUtils.extraAttributes(item) ?: return@on

                val id = data.getString("id").getOrNull() ?: return@on

                if (orbIds.contains(id)) Scheduler.scheduleTask {
                    val ent = minecraft.level?.getEntity(event.entityId)
                    if (ent != null) addQueue.add(Triple(ent.x, ent.y + 2.0, ent.z))
                    minecraft.level?.removeEntity(event.entityId, Entity.RemovalReason.DISCARDED)
                }
            }
        }

        on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundLevelParticlesPacket ?: return@on

            if (packet.particle.type != ParticleTypes.DUST) return@on

            if (packet.count != 0) return@on
            if (!packet.isOverrideLimiter) return@on
            if (!packet.alwaysShow()) return@on

            var l = addQueue.size
            while (--l >= 0) {
                val p = addQueue.poll() ?: break
                add(p.first, p.second, p.third)
            }
            val nearby = get(packet.x, packet.z) ?: return@on
            if (!nearby.any { it.matches(packet.x, packet.y, packet.z) }) return@on

            event.cancel()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        grid.clear()
        addQueue.clear()
    }
}