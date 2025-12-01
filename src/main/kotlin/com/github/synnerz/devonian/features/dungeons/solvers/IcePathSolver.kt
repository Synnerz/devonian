package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.monster.Silverfish
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.roundToInt

object IcePathSolver : Feature(
    "icePathSolver",
    "Draws a line to where you should hit the silverfish in ice path puzzle",
    "Dungeons",
    "catacombs"
) {
    private val solutions = listOf(
        // y is always 66
        IcePathSolution(8, 9, 12, 9),
        IcePathSolution(12, 9, 12, 8),
        IcePathSolution(12, 8, 20, 8),
        IcePathSolution(20, 8, 20, 24),
        IcePathSolution(20, 24, 19, 24),
        IcePathSolution(19, 24, 19, 23),
        IcePathSolution(19, 23, 21, 23),
        IcePathSolution(21, 23, 21, 14),
        IcePathSolution(21, 14, 14, 14),
        IcePathSolution(14, 14, 14, 25)
    )
    val currentSolution = ConcurrentLinkedQueue<IcePathSolution>()
    var inPath = false
    var silverfishEntity: Silverfish? = null
    var enteredAt = -1

    data class IcePathSolution(
        val x1: Int,
        val z1: Int,
        val x2: Int,
        val z2: Int
    )

    override fun initialize() {
        on<DungeonEvent.RoomEnter> {
            val room = it.room
            if (room.name != "Ice Path") return@on
            inPath = true
            enteredAt = EventBus.serverTicks()

            for (pos in solutions) {
                val ( x1, z1, x2, z2 ) = pos
                val pos1 = room.fromComp(x1, z1) ?: continue
                val pos2 = room.fromComp(x2, z2) ?: continue

                currentSolution.add(IcePathSolution(
                    pos1.first,
                    pos1.second,
                    pos2.first,
                    pos2.second
                ))
            }
        }

        on<DungeonEvent.RoomLeave> {
            if (!inPath) return@on
            inPath = false
            currentSolution.clear()
            enteredAt = -1
        }

        on<EntityJoinEvent> {
            val entity = it.entity
            if (entity !is Silverfish) return@on

            silverfishEntity = entity
        }

        on<TickEvent> {
            if (silverfishEntity == null || silverfishEntity!!.isDeadOrDying || currentSolution.isEmpty()) return@on

            val firstSolution = currentSolution.first() ?: return@on
            val x = (silverfishEntity!!.x + 0.5).roundToInt()
            val z = (silverfishEntity!!.z + 0.5).roundToInt()
            val dist = abs(x - firstSolution.x2) + abs(z - firstSolution.z2)
            if (dist > 2) return@on

            currentSolution.remove(firstSolution)
        }

        on<RenderWorldEvent> {
            if (currentSolution.isEmpty()) return@on

            currentSolution.forEachIndexed { idx, sol ->
                BlazeSolver.renderLine(
                    Vec3(sol.x1 + 0.5, 66.0, sol.z1 + 0.5),
                    Vec3(sol.x2 + 0.5, 66.0, sol.z2 + 0.5),
                    if (idx == 0) Color.GREEN else Color.RED
                )
            }
        }

        on<BlockPlaceEvent> { event ->
            if (enteredAt == -1 || !inPath) return@on

            val hitResult = event.blockHitResult
            if (hitResult.type == HitResult.Type.MISS) return@on

            val pos = hitResult.blockPos
            val x = pos.x
            val y = pos.y
            val z = pos.z
            val room = DungeonScanner.currentRoom ?: return@on
            val compPos = room.fromPos(x, z) ?: return@on
            if (compPos.first != 15 || y != 67 || compPos.second != 28) return@on

            val time = (EventBus.serverTicks() - enteredAt) * 0.05
            val seconds = "%.2fs".format(time)
            ChatUtils.sendMessage("&bIce Path took&f: &6$seconds", true)
            enteredAt = -1
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inPath = false
        silverfishEntity = null
        enteredAt = -1
        currentSolution.clear()
    }
}