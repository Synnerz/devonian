package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import java.util.*
import kotlin.math.abs
import kotlin.math.min

object DragonHitCount : Feature(
    "dragonHitCount",
    "tracks number of arrows you hit",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "M7",
    searchTags = setOf("arrow"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.WitherKing.isActiveState)
    }

    private var currType: M7Dragon? = null
    private var currId = 0
    private var hits = mutableListOf(0)

    override fun initialize() {
        on<M7Events.DragonSpawned2> { event ->
            currType = event.dragon
            currId = 0
            hits = mutableListOf(0)
        }

        on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is ClientboundAddEntityPacket -> {
                    if (packet.type != EntityType.ENDER_DRAGON) return@on
                    val type = M7Dragon.entries.minBy {
                        abs(it.path[0].x - packet.x) +
                        abs(it.path[0].y - packet.y) +
                        abs(it.path[0].z - packet.z)
                    }
                    if (type != currType) return@on
                    currId = packet.id
                }

                is ClientboundSoundPacket -> {
                    if (currId == 0) return@on
                    if (packet.sound.value() != SoundEvents.ARROW_HIT_PLAYER) return@on
                    if (packet.source != SoundSource.NEUTRAL) return@on
                    if (packet.volume != 1f) return@on
                    hits[hits.size - 1]++
                }
            }
        }

        on<TickEvent> {
            val id = currId
            if (id == 0) return@on

            val w = minecraft.level ?: return@on
            val ent = w.getEntity(id) as? EnderDragon?
            if (ent == null || ent.dragonDeathTime > 0) end()
        }

        on<ServerTickEvent> {
            if (currId == 0) return@on
            if (hits.size >= 90) end()
            else hits.add(0)
        }
    }

    private val db = EnumSet.of(DungeonClass.Healer, DungeonClass.Tank, DungeonClass.Mage)
    private fun end() {
        // dont conc mod please please please
        val isDb = db.contains(Dungeons.players.firstEntry()?.value?.role)

        var endI = 0
        var sum = 0
        var stack = if (isDb) -1 else 0
        hits.forEachIndexed { i, v ->
            if (v > 0) endI = i
            sum += v
            if (isDb) {
                if (sum >= 5 && stack == -1) stack = i
            } else if (i < 20) stack += v
        }

        if (isDb && stack == -1) stack = endI

        ChatUtils.sendMessage(
            "%s%s&7: &b%d &aarrows in &d%.2fs (%d ticks) &7| &b%d &aarrows in &d%.2fs (%d ticks)".format(
                currType?.textColor ?: "&0",
                currType?.name ?: "Unknown",
                sum,
                endI * 0.05,
                endI,
                if (isDb) min(sum, 5) else stack,
                (if (isDb) stack else min(20, endI)) * 0.05,
                (if (isDb) stack else min(20, endI)),
            )
        )

        currType = null
        currId = 0
        hits = mutableListOf(0)
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        currType = null
        currId = 0
        hits = mutableListOf(0)
    }
}