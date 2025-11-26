package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.level.block.Blocks
import org.joml.Vector3f
import java.awt.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object TeleportMazeSolver : Feature(
    "teleportMazeSolver",
    "Highlights the correct teleport pad to use inside the teleport maze puzzle",
    "Dungeons",
    "catacombs"
) {
    private val endFramePositions = listOf(
        // Y is always 69
        4 to 6,
        4 to 12,
        4 to 14,
        4 to 20,
        4 to 22,
        4 to 28,
        10 to 6,
        10 to 12,
        10 to 14,
        10 to 20,
        10 to 22,
        10 to 28,
        12 to 22,
        12 to 28,
        15 to 12,
        15 to 14,
        20 to 6,
        20 to 12,
        20 to 14,
        20 to 20,
        20 to 22,
        20 to 28,
        26 to 6,
        26 to 12,
        26 to 14,
        26 to 20,
        26 to 22,
        26 to 28
    )
    private val toRender = mutableListOf<Pair<Int, Int>>()
    var inMaze = false
    var cells: MutableList<Cell>? = null
    var orderedPads = mutableListOf<Pad>()
    var minX = 0
    var minZ = 0

    data class Pad(
        val x: Int,
        val z: Int,
        var cx: Int = 0,
        var cz: Int = 0,
        var angle: Double = 0.0,
        var blacklisted: Boolean = false
    )

    data class Cell(
        val gx: Int,
        val gz: Int,
        val pads: MutableList<Pad> = mutableListOf()
    ) {
        fun addPad(pad: Pad) = pads.add(pad)
    }

    override fun initialize() {
        on<DungeonEvent.RoomEnter> {
            val room = it.room
            if (room.name != "Teleport Maze") return@on
            inMaze = true

            val pads = mutableListOf<Pad>()
            for (pos in endFramePositions) {
                val x1 = pos.first
                val y = 69
                val z1 = pos.second
                val coords = room.fromComp(x1, z1) ?: continue
                val ( x, z ) = coords
                val blockState = WorldUtils.getBlockState(x, y, z) ?: continue
                if (blockState.block !== Blocks.END_PORTAL_FRAME) continue

                pads.add(Pad(x, z))
            }

            minX = (pads.minOfOrNull { pad -> pad.x } ?: 0)
            minZ = (pads.minOfOrNull { pad -> pad.z } ?: 0)
            cells = MutableList(9) { Cell(it / 3, it % 3) }

            for (pad in pads) {
                pad.cx = (pad.x - minX) / 8
                pad.cz = (pad.z - minZ) / 8
                val idx = pad.cx * 3 + pad.cz
                if (idx >= cells!!.size) continue
                cells!![idx].addPad(pad)
            }
        }

        on<DungeonEvent.RoomLeave> {
            if (!inMaze) return@on
            toRender.clear()
            inMaze = false
            minX = 0
            minZ = 0
            cells = null
            orderedPads.clear()
        }

        on<PacketReceivedEvent> { event ->
            val player = minecraft.player ?: return@on
            val packet = event.packet
            if (packet !is ClientboundPlayerPositionPacket) return@on

            val change = packet.change
            val pos = change.position
            val yaw = change.yRot

            if (pos.x % 0.5 != 0.0 || pos.y != 69.5 || pos.z % 0.5 != 0.0) return@on

            val newPad = padNear(pos.x.roundToInt(), pos.z.roundToInt()) ?: return@on
            val oldPad = padNear(player.x.roundToInt(), player.z.roundToInt()) ?: return@on

            newPad.blacklisted = true
            oldPad.blacklisted = true

            padAngle(pos.x.roundToInt(), pos.z.roundToInt(), yaw)
            padAngle(pos.x.toInt(), pos.z.toInt(), yaw)
        }

        on<RenderWorldEvent> {
            if (!inMaze) return@on
            if (orderedPads.isEmpty() || orderedPads.size < 2) return@on
            if (orderedPads.getOrNull(0)?.angle == orderedPads.getOrNull(1)?.angle) return@on

            val pos1 = orderedPads.getOrNull(0) ?: return@on
            Context.Immediate?.renderBox(
                pos1.x.toDouble(), 69.0, pos1.z.toDouble(),
                Color.GREEN,
                true
            )
            Context.Immediate?.renderFilledBox(
                pos1.x.toDouble(), 69.0, pos1.z.toDouble(),
                Color(0, 255, 0, 80),
                true
            )
            // less likely
            val pos2 = orderedPads.getOrNull(1) ?: return@on
            Context.Immediate?.renderBox(
                pos2.x.toDouble(), 69.0, pos2.z.toDouble(),
                Color.ORANGE,
                true
            )
            Context.Immediate?.renderFilledBox(
                pos2.x.toDouble(), 69.0, pos2.z.toDouble(),
                Color(255, 165, 0, 80),
                true
            )

            if (cells == null) return@on
            for (cell in cells!!) {
                for (pad in cell.pads) {
                    if (!pad.blacklisted) continue
                    Context.Immediate?.renderBox(
                        pad.x.toDouble(), 69.0, pad.z.toDouble(),
                        Color.RED,
                        true
                    )
                    Context.Immediate?.renderFilledBox(
                        pad.x.toDouble(), 69.0, pad.z.toDouble(),
                        Color(255, 0, 0, 80),
                        true
                    )
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        toRender.clear()
        inMaze = false
        minX = 0
        minZ = 0
        cells = null
        orderedPads.clear()
    }

    private fun cellAt(x: Int, z: Int): Cell? {
        if (x < minX || x > minX + 23 || z < minZ || z > minZ + 23) return null

        val cx = (x - minX) / 8
        val cz = (z - minZ) / 8

        return cells?.find { it.gx == cx && it.gz == cz }
    }

    private fun padNear(x: Int, z: Int): Pad? {
        val cell = cellAt(x, z) ?: return null
        var result: Pad? = null

        for (pad in cell.pads) {
            val dist = abs(x - pad.x) + abs(z - pad.z)
            if (dist <= 3) {
                result = pad
                break
            }
        }

        return result
    }

    // Whether the current pad is in the start or the end cell
    private fun inSpecial(pad: Pad): Boolean {
        if (cells == null) return false
        val endCell = cells!!.getOrNull(4)

        if (endCell != null && endCell.pads.contains(pad)) return true

        var result = false

        for (cell in cells!!) {
            if (cell.pads.size != 1 || cell == endCell || !cell.pads.contains(pad)) continue
            result = true
            break
        }

        return result
    }

    private fun fromPitchYaw(pitch: Float, yaw: Float): Vector3f {
        val radPitch = pitch * 0.017453292f
        val radYaw = yaw * 0.017453292f

        val f = cos(-radYaw - Math.PI.toFloat())
        val f1 = sin(-radYaw - Math.PI.toFloat())
        val f2 = -cos(-radPitch)
        val f3 = sin(-radPitch)

        return Vector3f(f1 * f2, f3, f * f2).normalize()
    }

    private fun padAngle(x: Int, z: Int, yaw: Float) {
        val vec = fromPitchYaw(0f, yaw)

        orderedPads.clear()
        for (cell in cells!!) {
            for (pad in cell.pads) {
                if (inSpecial(pad) || pad.blacklisted) continue
                val vec2 = Vector3f(pad.x + 0.5f - x, 0f, pad.z + 0.5f - z)
                val angle = Math.toDegrees(vec.angle(vec2).toDouble())
                pad.angle = angle
                orderedPads.add(pad)
            }
        }

        orderedPads.sortBy { it.angle }
    }
}