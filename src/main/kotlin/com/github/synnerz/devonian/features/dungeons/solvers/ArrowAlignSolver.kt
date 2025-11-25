package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.EntityInteractEvent
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import kotlinx.atomicfu.atomic
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.Items
import org.joml.Quaternionf
import kotlin.math.floor
import kotlin.math.max

object ArrowAlignSolver : Feature(
    "arrowAlignSolver",
    "s3 dev",
    "Dungeons",
    "catacombs"
) {
    private val SETTING_BLOCK_INCORRECT = addSelection(
        "blockClicks",
        "",
        "Block Incorrect Hits",
        listOf("Never", "Always", "WhenCrouching", "ExceptWhenCrouching"),
        0
    )

    private fun shouldBlockClicks() = when (SETTING_BLOCK_INCORRECT.getCurrent()) {
        "Always" -> true
        "WhenCrouching" -> minecraft?.player?.isShiftKeyDown ?: false
        "ExceptWhenCrouching" -> !(minecraft?.player?.isShiftKeyDown ?: true)
        else -> false
    }

    private val solutions = arrayOf(
        intArrayOf(7, 1, 1, 9, 9, 9, 9, 9, 7, 9, 3, 9, 7, 9, 9, 9, 7, 9, 3, 9, 7, 9, 9, 9, 7, 9, 3, 9, 7, 9, 9, 9, 9, 9, 3, 1, 1),
        intArrayOf(9, 1, 1, 1, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 1, 1, 1, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 1, 1, 1, 9),
        intArrayOf(5, 5, 7, 1, 1, 9, 9, 9, 3, 9, 7, 9, 3, 9, 9, 9, 3, 9, 9, 9, 3, 9, 9, 9, 3, 9, 9, 9, 3, 9, 9, 9, 9, 9, 9, 9, 9),
        intArrayOf(9, 9, 7, 1, 9, 9, 9, 9, 9, 1, 1, 9, 9, 9, 9, 9, 9, 9, 7, 1, 9, 9, 9, 9, 9, 1, 1, 9, 9, 9, 9, 9, 9, 9, 3, 1, 9),
        intArrayOf(9, 9, 9, 9, 9, 9, 9, 9, 9, 7, 9, 7, 9, 9, 9, 9, 7, 1, 9, 5, 7, 9, 9, 9, 7, 9, 9, 9, 7, 9, 9, 9, 5, 5, 9, 1, 1),
        intArrayOf(7, 1, 1, 9, 9, 9, 9, 9, 7, 9, 3, 9, 9, 9, 9, 9, 9, 9, 3, 9, 9, 9, 9, 9, 9, 9, 3, 9, 7, 9, 9, 9, 9, 9, 3, 1, 1),
        intArrayOf(5, 5, 7, 9, 9, 9, 9, 9, 3, 9, 7, 9, 7, 9, 9, 9, 3, 9, 9, 9, 7, 9, 9, 9, 3, 9, 9, 9, 7, 9, 9, 9, 3, 1, 1, 1, 1),
        intArrayOf(7, 1, 1, 9, 9, 9, 9, 9, 7, 9, 3, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 7, 9, 3, 9, 9, 9, 9, 9, 5, 5, 3),
        intArrayOf(9, 1, 9, 7, 9, 9, 9, 9, 9, 3, 9, 7, 9, 9, 9, 9, 9, 3, 9, 7, 9, 9, 9, 9, 9, 3, 9, 7, 9, 9, 9, 9, 9, 3, 1, 1, 9),
    )

    private var solution: IntArray? = null
    private val frameIds = atomic<Map<Int, Int>?>(null)
    private val frameState = IntArray(37) { 0 }
    private val clicksQueued = IntArray(37) { 0 }
    @Volatile
    private var atDev = false

    private fun getFrameId(y: Int, z: Int): Int {
        val dy = y - 120
        val dz = z - 75
        if (dy !in 0 .. 4) return -1
        if (dz !in 0 .. 4) return -1
        return (dy shl 3) or dz
    }

    private fun getClicks(id: Int): Int {
        val sol = solution ?: return 0
        val s = sol[id]
        if (s == 9) return 0
        val f: Int
        val c: Int
        synchronized(frameState) {
            f = frameState[id]
        }
        synchronized(clicksQueued) {
            c = clicksQueued[id]
        }
        return (sol[id] - f - c) and 7
    }

    override fun initialize() {
        on<TickEvent> {
            val player = minecraft.player ?: return@on
            atDev = Dungeons.floor.floorNum == 7 &&
                player.x in -2.0 .. 20.0 &&
                player.y in 100.0 .. 140.0 &&
                player.z in 50.0 .. 125.0

            if (!atDev) return@on
            if (solution != null) return@on

            val world = minecraft.level ?: return@on
            val frames = IntArray(37) { 9 }
            val ids = mutableMapOf<Int, Int>()
            world.entitiesForRendering().forEach { ent ->
                if (ent !is ItemFrame) return@forEach
                val x = floor(ent.x).toInt()
                if (x != -2) return@forEach
                val y = floor(ent.y).toInt()
                val z = floor(ent.z).toInt()

                val id = getFrameId(y, z)
                if (id == -1) return@forEach

                val item = ent.item
                if (item.item !== Items.ARROW) return@forEach

                ids[ent.id] = id
                frames[id] = ent.rotation
            }

            val sol = solutions.find { it.withIndex().all { (i, v) -> (v == 9) == (frames[i] == 9) } } ?: return@on
            solution = sol

            frameIds.value = ids
            synchronized(frameState) {
                frameState.fill(0)
            }
            synchronized(clicksQueued) {
                clicksQueued.fill(0)
            }
            sol.forEachIndexed { i, v ->
                if (v < 9) frameState[i] = frames[i]
            }
        }

        on<PacketReceivedEvent> { event ->
            if (!atDev) return@on
            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@on
            val entId = packet.id
            val map = frameIds.value ?: return@on
            val frameId = map[entId] ?: return@on

            packet.packedItems.forEach {
                if (it.id != 9) return@forEach
                val r = it.value as? Int ?: return@forEach
                val d: Int
                synchronized(frameState) {
                    d = (r - frameState[frameId]) and 7
                    frameState[frameId] = r
                }
                synchronized(clicksQueued) {
                    clicksQueued[frameId] = max(0, clicksQueued[frameId] - d)
                }
            }
        }

        on<EntityInteractEvent> { event ->
            if (!atDev) return@on
            val ent = event.entity as? ItemFrame ?: return@on

            val x = floor(ent.x).toInt()
            if (x != -2) return@on
            val y = floor(ent.y).toInt()
            val z = floor(ent.z).toInt()

            val id = getFrameId(y, z)
            if (id == -1) return@on

            val c = getClicks(id)
            if (shouldBlockClicks() && c == 0) event.cancel()
            else synchronized(clicksQueued) {
                clicksQueued[id]++
            }
        }

        on<RenderWorldEvent> {
            if (!atDev) return@on
            val ctx = Context.Immediate ?: return@on

            val textRenderer = minecraft.font
            val consumer = minecraft.renderBuffers().bufferSource()
            val layer = Font.DisplayMode.NORMAL
            val camPos = ctx.camera.position

            val scale = 0.03f
            val quat = Quaternionf(0.0, -0.7071067811865476, 0.0, 0.7071067811865476)

            for (y in 120 .. 124) {
                for (z in 75 .. 79) {
                    val id = getFrameId(y, z)
                    val n = getClicks(id)
                    if (n == 0) continue
                    val s = n.toString()
                    val offset = -textRenderer.width(s) * 0.5f

                    val dx = -1.9 - camPos.x
                    val dy = y + 0.5 - camPos.y
                    val dz = z + 0.5 - camPos.z

                    ctx.stacks.pushPose()
                    ctx.stacks.translate(dx, dy, dz)
                    ctx.stacks.last().rotate(quat)
                    ctx.stacks.scale(-scale, -scale, -scale)

                    textRenderer.drawInBatch(
                        s,
                        offset,
                        0f,
                        0xFFFFFFFF.toInt(),
                        true,
                        ctx.stacks.last().pose(),
                        consumer,
                        layer,
                        0,
                        LightTexture.FULL_BLOCK
                    )

                    consumer.endBatch()
                    ctx.stacks.popPose()
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        solution = null
        frameIds.value = null
        synchronized(frameState) {
            frameState.fill(0)
        }
        synchronized(clicksQueued) {
            clicksQueued.fill(0)
        }
    }
}