package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.PacketNameChangeEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.awt.Color

object BlazeSolver : Feature(
    "blazeSolver",
    "Highlights the correct blaze to shoot in blaze puzzle",
    "Dungeons",
    "catacombs"
) {
    private val blazeHpRegex = "^\\[Lv15] ♨ Blaze [\\d,]+/([\\d,]+)❤$".toRegex()
    private val entityList = mutableMapOf<Int, Int>() // <entityId>: <MaxHP>
    var hasPlatform = false
    var inBlaze = false
    val blazes = mutableListOf<BlazeEntity>()
    var lastBlazes = 0
    var startedAt = 0L
    var efficientPos: Triple<Int, Int, Int>? = null

    data class BlazeEntity(val entity: Entity, val maxHP: Int)

    override fun initialize() {
        on<DungeonEvent.RoomEnter> { event ->
            val room = event.room
            if (room.name != "Blaze") return@on
            val platformPos = room.fromComp(15, 14) ?: return@on
            val blockState = WorldUtils.getBlockState(platformPos.first, 118, platformPos.second) ?: return@on
            inBlaze = true
            hasPlatform = blockState.block == Blocks.COBBLESTONE

            val rcomp = room.fromComp(10, 19) ?: return@on
            efficientPos = Triple(
                rcomp.first,
                if (hasPlatform) 94 else 44,
                rcomp.second
            )
        }

        on<DungeonEvent.RoomLeave> {
            if (inBlaze) blazes.clear()
            if (inBlaze) entityList.clear()
            inBlaze = false
            hasPlatform = false
            startedAt = 0L
            lastBlazes = 0
            efficientPos = null
        }

        on<TickEvent> {
            if (!inBlaze) return@on
            blazes.clear()

            entityList.entries.forEach {
                val entityId = it.key
                val maxHP = it.value
                val entity = minecraft.level?.getEntity(entityId - 1) ?: return@forEach
                blazes.add(BlazeEntity(entity, maxHP))
            }

            if (blazes.size == 9 && startedAt == 0L) startedAt = System.currentTimeMillis()
            if (blazes.isEmpty() && startedAt != 0L && lastBlazes == 1) {
                val time = (System.currentTimeMillis() - startedAt) / 1000f
                val seconds = "%.2fs".format(time)
                ChatUtils.sendMessage("&bBlaze took&f: &6$seconds", true)
                blazes.clear()
                entityList.clear()
                inBlaze = false
                hasPlatform = false
                startedAt = 0L
                lastBlazes = 0
                efficientPos = null
                return@on
            }

            blazes.sortBy { it.maxHP }

            // if it doesn't have platform, reverse the list
            if (!hasPlatform) blazes.reverse()
            lastBlazes = blazes.size
        }

        on<PacketNameChangeEvent> { event ->
            if (event.type !== EntityType.ARMOR_STAND) return@on

            val name = event.name
            val entityId = event.entityId
            val match = blazeHpRegex.matchEntire(name) ?: return@on
            val maxHp = match.groupValues[1].replace(",", "").toInt()

            entityList[entityId] = maxHp
        }

        on<RenderWorldEvent> {
            if (efficientPos != null) {
                Context.Immediate?.renderFilledBox(
                    efficientPos!!.first.toDouble(), efficientPos!!.second.toDouble(), efficientPos!!.third.toDouble(),
                    color = Color(0, 255, 0, 80),
                    phase = true
                )

                Context.Immediate?.renderBox(
                    efficientPos!!.first.toDouble(), efficientPos!!.second.toDouble(), efficientPos!!.third.toDouble(),
                    color = Color.GREEN,
                    phase = true
                )
            }
            // yes i could make this dynamic, but why ?
            // it is pointless if we only need 3 entries
            val blaze = blazes.getOrNull(0) ?: return@on
            highlightBlaze(blaze.entity)

            val blaze2 = blazes.getOrNull(1) ?: return@on
            highlightBlaze(blaze2.entity, Color.ORANGE, Color(255, 165, 0, 80))

            renderLine(
                blaze.entity.position(),
                blaze2.entity.position(),
                Color.GREEN,
                Color.ORANGE
            )

            val blaze3 = blazes.getOrNull(2) ?: return@on
            highlightBlaze(blaze3.entity, Color.RED, Color(255, 0, 0, 80))

            renderLine(
                blaze2.entity.position(),
                blaze3.entity.position(),
                Color.ORANGE,
                Color.RED
            )
        }
    }

    private fun lerp(c: Double, l: Double, t: Float = minecraft.deltaTracker.getGameTimeDeltaPartialTick(false))
        = l + (c - l) * t

    private fun highlightBlaze(
        entity: Entity,
        outlineColor: Color = Color.GREEN,
        filledColor: Color = Color(0, 255, 0, 80)
    ) {
        Context.Immediate?.renderFilledBox(
            entity.x - 0.5, entity.y, entity.z - 0.5,
            0.9,
            entity.bbHeight.toDouble(),
            filledColor
        )
        Context.Immediate?.renderBox(
            entity.x - 0.5, entity.y, entity.z - 0.5,
            0.9,
            entity.bbHeight.toDouble(),
            outlineColor,
            phase = true
        )
    }

    fun renderLine(
        pos1: Vec3,
        pos2: Vec3,
        colorStart: Color = Color.CYAN,
        colorEnd: Color = colorStart
    ) {
        val x1 = pos1.x.toFloat()
        val x2 = pos2.x.toFloat()
        val y1 = pos1.y.toFloat()
        val y2 = pos2.y.toFloat()
        val z1 = pos1.z.toFloat()
        val z2 = pos2.z.toFloat()

        val normalized = Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize()
        val consumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.LINE_STRIP)
        val camPos = minecraft.cameraEntity ?: return
        val stack = PoseStack()
        stack.pushPose()
        stack.translate(
            -lerp(camPos.x, camPos.xo),
            -lerp(camPos.y, camPos.yo),
            -lerp(camPos.z, camPos.zo)
        )

        consumer
            .addVertex(stack.last(), x1, y1, z1)
            .setColor(colorStart.rgb)
            .setNormal(stack.last(), normalized)

        consumer
            .addVertex(stack.last(), x2, y2, z2)
            .setColor(colorEnd.rgb)
            .setNormal(stack.last(), normalized)

        stack.popPose()
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inBlaze = false
        hasPlatform = false
        startedAt = 0L
        lastBlazes = 0
        efficientPos = null
        blazes.clear()
        entityList.clear()
    }
}