package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.MapItem
import java.awt.Color
import kotlin.math.floor

object TicTacToeSolver : Feature(
    "ticTacToeSolver",
    "Highlights the most \"efficient\" button to press to complete the tictactoe puzzle",
    "Dungeons",
    "catacombs"
) {
    private val completedPuzzleRegex = "^PUZZLE SOLVED! \\w+ tied Tic Tac Toe! Good job!$".toRegex()
    private val failedPuzzleRegex = "^PUZZLE FAIL! \\w+ lost Tic Tac Toe! Yikes!$".toRegex()
    private val boardPos = listOf(
        Triple(8, 72, 17), Triple(8, 72, 16), Triple(8, 72, 15),
        Triple(8, 71, 17), Triple(8, 71, 16), Triple(8, 71, 15),
        Triple(8, 70, 17), Triple(8, 70, 16), Triple(8, 70, 15)
    )
    var entityPositions = mutableListOf<TicTacToePlayer>()
    var currentBoard = mutableListOf<String?>(
        null, null, null,
        null, null, null,
        null, null, null
    )
    var inTTT = false
    var hasMoved = false
    var lastStatus: String? = null
    var currentBestMove = -1

    data class TicTacToePlayer(
        val x: Int,
        val y: Int,
        val z: Int,
        val status: String
    )

    override fun initialize() {
        on<DungeonEvent.RoomEnter> {
            val room = it.room
            if (room.name != "Tic Tac Toe") return@on

            inTTT = true
        }

        on<DungeonEvent.RoomLeave> {
            if (!inTTT) return@on
            inTTT = false
        }

        on<ChatEvent> { event ->
            event.matches(failedPuzzleRegex)?.let {
                reset()
                return@on
            }

            event.matches(completedPuzzleRegex) ?: return@on

            reset()
        }

        on<EntityJoinEvent> {
            val entity = it.entity

            Scheduler.scheduleServerTask(4) {
                if (entity !is ItemFrame) return@scheduleServerTask
                if (!entity.hasFramedMap()) return@scheduleServerTask
                val mapId = entity.getFramedMapId(entity.item) ?: return@scheduleServerTask
                val map = MapItem.getSavedData(mapId, minecraft.level) ?: return@scheduleServerTask
                val colors = map.colors
                val idx = colors.indexOf(114)
                if (idx == -1) return@scheduleServerTask

                val status = if (idx == 2700) "X" else "O"
                entityPositions.add(TicTacToePlayer(
                    floor(entity.x).toInt(),
                    entity.y.toInt(),
                    floor(entity.z).toInt(),
                    status
                ))
                currentBestMove = -1
                lastStatus = status
                hasMoved = true
            }
        }

        on<TickEvent> {
            if (!inTTT || !hasMoved) return@on
            val room = DungeonScanner.currentRoom ?: return@on

            currentBoard.fill(null)

            for (pos in entityPositions) {
                val compPos = room.fromPos(pos.x, pos.z) ?: continue
                val trip = Triple(compPos.first, pos.y, compPos.second)
                val idx = boardPos.indexOf(trip)
                if (idx == -1) continue

                currentBoard[idx] = pos.status
            }

            hasMoved = false
            if (lastStatus == "X") onAIMove(currentBoard)
            lastStatus = null
        }

        on<RenderWorldEvent> {
            if (currentBestMove == -1 || !inTTT) return@on

            val currentRoom = DungeonScanner.currentRoom ?: return@on
            val bestMove = boardPos.getOrNull(currentBestMove) ?: return@on
            val roomPos = currentRoom.fromComp(bestMove.first - 1, bestMove.third) ?: return@on

            Context.Immediate?.renderBox(
                roomPos.first.toDouble(), bestMove.second.toDouble(), roomPos.second.toDouble(),
                Color.GREEN,
                true
            )
            Context.Immediate?.renderFilledBox(
                roomPos.first.toDouble(), bestMove.second.toDouble(), roomPos.second.toDouble(),
                Color(0, 255, 0, 80)
            )
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inTTT = false
        reset()
    }

    private fun reset() {
        currentBoard.fill(null)
        entityPositions.clear()
        hasMoved = false
        currentBestMove = -1
        lastStatus = null
    }

    private fun onAIMove(board: List<String?>) {
        currentBestMove = bestMove(board, "O")
    }

    // Algorithm
    private val boardOrder = listOf(4, 0, 2, 6, 8, 1, 3, 5, 7)
    private val winningSides = listOf(
        Triple(0, 1, 2), Triple(3, 4, 5), Triple(6, 7, 8),
        Triple(0, 3, 6), Triple(1, 4, 7), Triple(2, 5, 8),
        Triple(0, 4, 8), Triple(2, 4, 6)
    )

    private fun isWinner(board: List<String?>, player: String): Boolean {
        return winningSides.any { (a, b, c) ->
            board[a] == player && board[b] == player && board[c] == player
        }
    }

    // Alpha beta pruning
    private fun minMax(
        board: List<String?>,
        depth: Int,
        alpha: Int,
        beta: Int,
        isPlayer: Boolean
    ): Int {
        if (isWinner(board, "X")) return 10 - depth
        if (isWinner(board, "O")) return depth - 10
        if (board.all { it != null }) return 0

        var a = alpha
        var b = beta

        if (isPlayer) {
            var best = Int.MIN_VALUE

            for (idx in boardOrder) {
                if (board[idx] != null) continue
                val tempBoard = board.mapIndexed { jdx, cell -> if (idx == jdx) "X" else cell }
                val score = minMax(tempBoard, depth + 1, a, b, false)
                best = maxOf(best, score)
                a = maxOf(a, score)
                if (b <= a) break
            }

            return best
        }

        var best = Int.MAX_VALUE

        for (idx in boardOrder) {
            if (board[idx] != null) continue
            val tempBoard = board.mapIndexed { jdx, cell -> if (idx == jdx) "O" else cell }
            val score = minMax(tempBoard, depth + 1, a, b, true)
            best = minOf(best, score)
            b = minOf(b, score)
            if (b <= a) break
        }

        return best
    }

    private fun bestMove(
        board: List<String?>,
        player: String
    ): Int {
        val maximizing = player == "X"
        var bestScore = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE
        var bestMove = -1

        // If there's only 1 player in the board
        // that means we can hard code the actual player
        // to go towards middle or top left
        if (board.filterNotNull().size == 1) {
            if (board[4] == null) return 4
            return 0
        }

        for (idx in boardOrder) {
            if (board[idx] != null) continue
            val tempBoard = board.mapIndexed { jdx, cell -> if (idx == jdx) player else cell }
            val score = minMax(tempBoard, 0, Int.MIN_VALUE, Int.MAX_VALUE, player != "X")

            if (maximizing) {
                if (score > bestScore) {
                    bestScore = score
                    bestMove = idx
                }
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMove = idx
                }
            }
        }

        return bestMove
    }
}