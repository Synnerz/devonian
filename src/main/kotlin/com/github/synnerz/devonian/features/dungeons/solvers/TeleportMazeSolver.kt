package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.phys.HitResult
import java.awt.Color
import kotlin.math.*

object TeleportMazeSolver : Feature(
    "teleportMazeSolver",
    "Highlights the correct teleport pad to use inside the teleport maze puzzle.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
    searchTags = setOf("tp", "puzzle"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val endFramePositions = listOf(
        // Y is always 69
        CompPad(4, 6, 5, 7),
        CompPad(4, 12, 5, 11),
        CompPad(4, 14, 5, 15),
        CompPad(4, 20, 5, 19),
        CompPad(4, 22, 5, 23),
        CompPad(4, 28, 5, 27),
        CompPad(10, 6, 9, 7),
        CompPad(10, 12, 9, 11),
        CompPad(10, 14, 9, 15),
        CompPad(10, 20, 9, 19),
        CompPad(10, 22, 9, 23),
        CompPad(10, 28, 9, 27),
        CompPad(12, 22, 13, 23),
        CompPad(12, 28, 13, 27),
        CompPad(18, 22, 17, 23),
        CompPad(18, 28, 17, 27),
        CompPad(20, 6, 21, 7),
        CompPad(20, 12, 21, 11),
        CompPad(20, 14, 21, 15),
        CompPad(20, 20, 21, 19),
        CompPad(20, 22, 21, 23),
        CompPad(20, 28, 21, 27),
        CompPad(26, 6, 25, 7),
        CompPad(26, 12, 25, 11),
        CompPad(26, 14, 25, 15),
        CompPad(26, 20, 25, 19),
        CompPad(26, 22, 25, 23),
        CompPad(26, 28, 25, 27),

        CompPad(15, 12, 14, 11, special = true),
        CompPad(15, 14, 16, 15, special = true, isEnd = true),
    )
    private var inMaze = false
    private var enteredAt = -1
    private val pads = mutableListOf<Pad>()

    private data class CompPad(
        val cx: Int,
        val cz: Int,
        val tx: Int,
        val tz: Int,
        val special: Boolean = false,
        val isEnd: Boolean = false,
    )
    private data class Pad(
        val x: Int,
        val z: Int,
        val tx: Int,
        val tz: Int,
        val special: Boolean,
        val isEnd: Boolean,
    ) {
        var visited = false
        var correct = false
        var possible = false
        var incorrect = false
    }

    override fun initialize() {
        on<DungeonEvent.RoomEnter> {
            val room = it.room
            if (room.name != "Teleport Maze") return@on

            inMaze = true
            enteredAt = EventBus.serverTicks()
            if (!room.hasRotation()) return@on

            pads.clear()
            for (comp in endFramePositions) {
                val (x, z) = room.fromComp(comp.cx, comp.cz) ?: continue
                val (tx, tz) = room.fromComp(comp.tx, comp.tz) ?: continue

                pads.add(Pad(x, z, tx, tz, comp.special, comp.isEnd))
            }
        }

        on<DungeonEvent.RoomLeave> {
            reset()
        }

        on<PacketReceivedEvent> { event ->
            if (!inMaze) return@on

            val packet = event.packet
            if (packet !is ClientboundPlayerPositionPacket) return@on

            if (packet.relatives.isNotEmpty()) return@on

            val change = packet.change
            val pos = change.position
            val yaw = change.yRot

            if (pos.x % 0.5 != 0.0 || pos.y != 69.5 || pos.z % 0.5 != 0.0) return@on

            Scheduler.scheduleBeforePacket {
                val player = minecraft.player ?: return@scheduleBeforePacket
                val oldPad = pads.minByOrNull { abs(it.x - player.x) + abs(it.z - player.z) } ?: return@scheduleBeforePacket
                val newPad = pads.minByOrNull { abs(it.x - pos.x) + abs(it.z - pos.z) } ?: return@scheduleBeforePacket

                if (newPad.special) {
                    pads.forEach {
                        it.visited = false
                        it.correct = false
                        it.possible = false
                        it.incorrect = false
                    }
                    if (newPad.isEnd) return@scheduleBeforePacket
                }
                else {
                    oldPad.visited = true
                    newPad.visited = true
                }

                val dir = Vec2(cos((yaw + 90.0) / 180.0 * PI), sin((yaw + 90.0) / 180.0 * PI))
                pads.forEach {
                    if (it === newPad) return@forEach
                    if (it.special) return@forEach

                    val offset = Vec2(
                        (it.tx - newPad.tx).toDouble(),
                        (it.tz - newPad.tz).toDouble(),
                    )
                    val matches = dir.parallel(offset)
                    it.correct = matches && !it.incorrect
                    it.possible = it.possible || matches
                    it.incorrect = it.incorrect || !matches
                }
            }
        }

        on<RenderWorldEvent> {
            if (!inMaze) return@on

            pads.forEach {
                val color = if (it.correct) Color.GREEN
                    else if (it.visited) Color.RED
                    else if (it.possible) Color.ORANGE
                    else return@forEach
                val colorFill = Color(color.red, color.green, color.blue, 80)

                Render3DImmediate.renderWireframeBox(
                    it.x.toDouble(), 69.0, it.z.toDouble(),
                    1.0, 1.0,
                    color,
                    phase = false,
                    lineWidth = 2.0,
                )
                Render3DImmediate.renderFilledBox(
                    it.x.toDouble(), 69.0, it.z.toDouble(),
                    1.0, 1.0,
                    colorFill,
                    phase = false,
                )
            }
        }

        on<UseItemOnEvent> { event ->
            if (enteredAt == -1 || !inMaze) return@on

            val hitResult = event.blockHitResult
            if (hitResult.type == HitResult.Type.MISS) return@on

            val pos = hitResult.blockPos
            val x = pos.x
            val y = pos.y
            val z = pos.z
            val room = DungeonScanner.currentRoom ?: return@on
            val compPos = room.fromPos(x, z) ?: return@on
            if (compPos.first != 15 || y != 70 || compPos.second != 20 || !PuzzleTimers.isEnabled()) return@on

            val time = (EventBus.serverTicks() - enteredAt) * 0.05
            val seconds = "%.2fs".format(time)
            ChatUtils.sendMessage("&bTeleport Maze took&f: &6$seconds", true)
            enteredAt = -1
        }
    }

    private fun reset() {
        inMaze = false
        enteredAt = -1
        pads.clear()
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        reset()
    }

    private data class Vec2(val u: Double, val v: Double)  {
        fun parallel(o: Vec2): Boolean {
            if (u.eq(0.0)) return o.u.eq(0.0) && v.sign == o.v.sign
            if (v.eq(0.0)) return o.v.eq(0.0) && u.sign == o.u.sign
            return (u * o.v).eq(v * o.u) && u.sign == o.u.sign
        }

        companion object {
            private fun Double.eq(o: Double) = abs(this - o) < 1e-2
        }
    }
}