package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.min

object DragonHitCount : Feature(
    "dragonHitCount",
    "tracks number of arrows you hit",
    Categories.M7,
    "catacombs",
    searchTags = setOf("arrow"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.WitherKing.isActiveState)
    }

    private var currType: M7Dragon? = null
    private var hits = CopyOnWriteArrayList(mutableListOf(0))

    override fun initialize() {
        on<M7Events.DragonSpawned> { event ->
            currType = event.dragon.type
            hits = CopyOnWriteArrayList(mutableListOf(0))
        }

        on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundSoundPacket ?: return@on
            if (currType == null) return@on

            if (packet.sound.value() != SoundEvents.ARROW_HIT_PLAYER) return@on
            if (packet.source != SoundSource.NEUTRAL) return@on
            if (packet.volume != 1f) return@on
            hits[hits.size - 1]++
        }

        on<M7Events.DragonDeath> {
            end()
        }

        on<ServerTickEvent> {
            if (currType == null) return@on
            if (hits.size >= 90) end()
            else hits.add(0)
        }
    }

    private val db = EnumSet.of(DungeonClass.Healer, DungeonClass.Tank, DungeonClass.Mage)
    private fun end() {
        val isDb = db.contains(Dungeons.selfPlayer.role)

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
        hits = CopyOnWriteArrayList(mutableListOf(0))
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        currType = null
        hits = CopyOnWriteArrayList(mutableListOf(0))
    }
}