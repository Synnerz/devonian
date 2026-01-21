package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import kotlinx.atomicfu.atomic
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.Items
import org.joml.Quaternionf
import kotlin.math.floor
import kotlin.math.max

object ArrowAlignSolver : Feature(
    "arrowAlignSolver",
    "s3 dev",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
) {
    private val SETTING_BLOCK_INCORRECT = addSelection(
        "blockClicks",
        0,
        listOf("Never", "Always", "WhenCrouching", "ExceptWhenCrouching"),
        "",
        "Block Incorrect Hits",
    )

    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F7.isActiveState, Stages.S3.hasFinishedState.map(Boolean::not))
    }

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
    private val PREVENTED_SOUND = SoundEvents.NOTE_BLOCK_BASS

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
            atDev =
                player.x in -2.0 .. 20.0 &&
                player.y in 100.0 .. 140.0 &&
                player.z in 50.0 .. 125.0

            if (!atDev) {
                solution = null
                return@on
            }
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
            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@on
            val entId = packet.id
            val map = frameIds.value ?: return@on
            val frameId = map[entId] ?: return@on

            packet.packedItems.forEach {
                if (it.id != 10) return@forEach
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
            if (shouldBlockClicks() && c == 0) {
                event.cancel()
                minecraft.level?.playPlayerSound(
                    PREVENTED_SOUND.value(),
                    SoundSource.MASTER,
                    1f, 0.5f,
                )
            } else synchronized(clicksQueued) {
                clicksQueued[id]++
            }
        }

        on<RenderWorldEvent> {
            if (!atDev) return@on

            val textRenderer = minecraft.font
            val consumer = minecraft.renderBuffers().bufferSource()
            val layer = Font.DisplayMode.NORMAL
            val camPos = Render3DImmediate.camera.pos

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

                    Render3DImmediate.poseStack.pushPose()
                    Render3DImmediate.poseStack.translate(dx, dy, dz)
                    Render3DImmediate.poseStack.last().rotate(quat)
                    Render3DImmediate.poseStack.scale(-scale, -scale, -scale)

                    textRenderer.drawInBatch(
                        s,
                        offset,
                        0f,
                        0xFFFFFFFF.toInt(),
                        true,
                        Render3DImmediate.poseStack.last().pose(),
                        consumer,
                        layer,
                        0,
                        LightTexture.FULL_BLOCK
                    )

                    consumer.endBatch()
                    Render3DImmediate.poseStack.popPose()
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