package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.MapItem
import net.minecraft.world.phys.AABB
import java.awt.Color
import kotlin.math.floor

object TicTacToeSolver : Feature(
    "ticTacToeSolver",
    "Highlights the most \"efficient\" button to press to complete the tictactoe puzzle.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
    searchTags = setOf("ttt", "puzzle"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val SETTING_SEND_MSG = addSwitch(
        "sendMsg",
        false,
        "Sends \"Tic Tac Toe done\" in party chat",
        "TTT Done Message"
    )
    private val SETTING_PREDICT_NEXT = addSwitch(
        "predictNext",
        false,
        "Predicts the next best move and highlights it in orange (NOTE: may not be 100% accurate)",
        "TTT Prediction"
    )
    private val completedPuzzleRegex = "^PUZZLE SOLVED! \\w+ tied Tic Tac Toe! Good job!$".toRegex()
    private val failedPuzzleRegex = "^PUZZLE FAIL! \\w+ lost Tic Tac Toe! Yikes!$".toRegex()
    private val resetPuzzleRegex = "^You used the Architect's First Draft to reset Tic Tac Toe!$".toRegex()
    private val boardPos = listOf(
        Triple(8, 72, 17), Triple(8, 72, 16), Triple(8, 72, 15),
        Triple(8, 71, 17), Triple(8, 71, 16), Triple(8, 71, 15),
        Triple(8, 70, 17), Triple(8, 70, 16), Triple(8, 70, 15)
    )
    var currentBoard = mutableListOf<String?>(
        null, null, null,
        null, null, null,
        null, null, null
    )
    var inTTT = false
    var hasMoved = false
    var lastStatus: String? = null
    var currentBestMove = -1
    var predictedMove = -1
    var enteredAt = -1
    var hasSent = false

    override fun initialize() {
        on<DungeonEvent.RoomEnter> {
            val room = it.room
            if (room.name != "Tic Tac Toe") return@on

            inTTT = true
            enteredAt = EventBus.serverTicks()
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
            event.matches(resetPuzzleRegex)?.let {
                currentBoard.fill(null)
                enteredAt = EventBus.serverTicks()
                return@on
            }

            event.matches(completedPuzzleRegex) ?: return@on
            if (!PuzzleTimers.isEnabled() || enteredAt == -1) return@on

            val time = (EventBus.serverTicks() - enteredAt) * 0.05
            val seconds = "%.2fs".format(time)
            ChatUtils.sendMessage("&bTic Tac Toe took&f: &6$seconds", true)
            reset()
        }

        on<TickEvent> {
            if (!inTTT) return@on
            val room = DungeonScanner.currentRoom ?: return@on
            val start = Triple(8, 70, 15)
            val end = Triple(8, 72, 17)
            val ( startX, startZ ) = room.fromComp(start.first - 1, start.third - 1) ?: return@on
            val ( endX, endZ ) = room.fromComp(end.first + 1, end.third + 1) ?: return@on

            val itemMaps = minecraft.level?.getEntitiesOfClass(
                ItemFrame::class.java,
                AABB(
                    startX.toDouble(), start.second.toDouble() - 1, startZ.toDouble(),
                    endX.toDouble(), end.second.toDouble() + 1, endZ.toDouble()
                )
            ) ?: return@on

            val board = mutableListOf<String?>(
                null, null, null,
                null, null, null,
                null, null, null
            )
            itemMaps.forEach { entity ->
                val compPos = room.fromPos(floor(entity.x).toInt(), floor(entity.z).toInt()) ?: return@forEach
                if (compPos.first !in 6..9) return@forEach
                val idx = boardPos.indexOf(Triple(compPos.first, entity.y.toInt(), compPos.second))

                if (idx == -1) return@forEach
                if (!entity.hasFramedMap()) return@forEach

                val mapId = entity.getFramedMapId(entity.item)
                val level = minecraft.level ?: return@forEach
                val map = MapItem.getSavedData(mapId, level) ?: return@forEach
                val colors = map.colors
                val jdx = colors.indexOf(114)
                if (jdx == -1) return@forEach

                val status = if (jdx == 2700) "X" else "O"
                board[idx] = status
                if (currentBoard[idx] != status) {
                    currentBestMove = -1
                    lastStatus = status
                    hasMoved = true
                }
            }
            if (!hasMoved) return@on

            currentBoard.fill(null)
            currentBoard = board

            hasMoved = false
            if (lastStatus == "X") onAIMove(currentBoard)
            if (currentBoard.filterNotNull().size == 8 && SETTING_SEND_MSG.get() && !hasSent) {
                hasSent = true
                ChatUtils.command("pc Tic Tac Toe done")
            }
            lastStatus = null
        }

        on<RenderWorldEvent> {
            if (!inTTT) return@on

            if (SETTING_PREDICT_NEXT.get() && predictedMove != -1) {
                val currentRoom = DungeonScanner.currentRoom ?: return@on
                val bestMove = boardPos.getOrNull(predictedMove) ?: return@on
                val roomPos = currentRoom.fromComp(bestMove.first - 1, bestMove.third) ?: return@on

                Render3DImmediate.renderWireframeBox(
                    roomPos.first.toDouble(), bestMove.second.toDouble(), roomPos.second.toDouble(),
                    1.0, 1.0,
                    Color.ORANGE,
                    phase = false,
                )
                Render3DImmediate.renderFilledBox(
                    roomPos.first.toDouble(), bestMove.second.toDouble(), roomPos.second.toDouble(),
                    1.0, 1.0,
                    Color(255, 165, 0, 80),
                )
            }
            if (currentBestMove == -1) return@on

            val currentRoom = DungeonScanner.currentRoom ?: return@on
            val bestMove = boardPos.getOrNull(currentBestMove) ?: return@on
            val roomPos = currentRoom.fromComp(bestMove.first - 1, bestMove.third) ?: return@on

            Render3DImmediate.renderWireframeBox(
                roomPos.first.toDouble(), bestMove.second.toDouble(), roomPos.second.toDouble(),
                1.0, 1.0,
                Color.GREEN,
                phase = false,
            )
            Render3DImmediate.renderFilledBox(
                roomPos.first.toDouble(), bestMove.second.toDouble(), roomPos.second.toDouble(),
                1.0, 1.0,
                Color(0, 255, 0, 80),
            )
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inTTT = false
        hasSent = false
        reset()
    }

    private fun reset() {
        currentBoard.fill(null)
        hasMoved = false
        currentBestMove = -1
        predictedMove = -1
        lastStatus = null
        enteredAt = -1
    }

    private fun onAIMove(board: List<String?>) {
        currentBestMove = bestMove(board, "O")
        if (!SETTING_PREDICT_NEXT.get() || board.filterNotNull().isEmpty()) return

        val nextBoard = buildList {
            board.forEachIndexed { idx, str -> add(if (idx == currentBestMove) "O" else str) }
        }
        val predictX = bestMove(nextBoard, "X")
        val postBoard = buildList {
            nextBoard.forEachIndexed { idx, str -> add(if (idx == predictX) "X" else str) }
        }
        predictedMove = bestMove(postBoard, "O")
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