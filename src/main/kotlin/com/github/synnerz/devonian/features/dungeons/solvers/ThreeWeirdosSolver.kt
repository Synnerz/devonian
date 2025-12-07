package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

object ThreeWeirdosSolver : Feature(
    "threeWeirdosSolver",
    "Highlights the correct chest in the three weirdos puzzle room as well as changing the color of the text in chat",
    "Dungeons",
    "catacombs"
) {
    private val npcRegex = "^\\[NPC] (\\w+): (.*)".toRegex()
    private val completedRegex = "^PUZZLE SOLVED! \\w+ wasn't fooled by \\w+! Good job!$".toRegex()
    private val failedRegex = "^PUZZLE FAIL! \\w+ was fooled by \\w+! Yikes!$".toRegex()
    private val solutions = listOf(
        "The reward is not in my chest!".toRegex(),
        "At least one of them is lying, and the reward is not in \\w+'s chest.?".toRegex(),
        "My chest doesn't have the reward\\. We are all telling the truth.?".toRegex(),
        "My chest has the reward and I'm telling the truth!".toRegex(),
        "The reward isn't in any of our chests.?".toRegex(),
        "Both of them are telling the truth\\. Also, \\w+ has the reward in their chest.?".toRegex()
    )
    private val wrongAnswers = listOf(
        "One of us is telling the truth!".toRegex(),
        "They are both telling the truth\\. The reward isn't in \\w+'s chest.".toRegex(),
        "We are all telling the truth!".toRegex(),
        "\\w+ is telling the truth and the reward is in his chest.".toRegex(),
        "My chest doesn't have the reward. At least one of the others is telling the truth!".toRegex(),
        "One of the others is lying.".toRegex(),
        "They are both telling the truth, the reward is in \\w+'s chest.".toRegex(),
        "They are both lying, the reward is in my chest!".toRegex(),
        "The reward is in my chest.".toRegex(),
        "The reward is not in my chest\\. They are both lying.".toRegex(),
        "\\w+ is telling the truth.".toRegex(),
        "My chest has the reward.".toRegex()
    )
    private val dirs = listOf(
        1 to 0,
        -1 to 0,
        0 to 1,
        0 to -1
    )
    private val CORRECT_OUTLINE_COLOR = Color(0, 255, 0, 255)
    private val CORRECT_FILLED_COLOR = Color(0, 255, 0, 80)
    private val WRONG_OUTLINE_COLOR = Color(255, 0, 0, 255)
    private val WRONG_FILLED_COLOR = Color(255, 0, 0, 80)
    private val entityList = ConcurrentHashMap<String, Int>()
    val answers = mutableListOf<AnswerData>()
    var enteredAt = -1

    data class AnswerData(
        var entityPos: Vec3,
        var chestPos: Pair<Double, Double>,
        var isCorrect: Boolean
    )

    override fun initialize() {
        on<DungeonEvent.RoomEnter> {
            val room = it.room
            if (room.name != "Three Weirdos") return@on
            enteredAt = EventBus.serverTicks()
        }

        on<DungeonEvent.RoomLeave> {
            if (enteredAt == -1) return@on
            enteredAt = -1
        }

        on<ChatEvent> { event ->
            event.matches(completedRegex)?.let {
                val time = (EventBus.serverTicks() - enteredAt) * 0.05
                val seconds = "%.2fs".format(time)
                ChatUtils.sendMessage("&bThree Weirdos took&f: &6$seconds", true)
                reset()
                return@on
            }

            event.matches(failedRegex)?.let {
                reset()
                return@on
            }

            val match = event.matches(npcRegex) ?: return@on
            val ( name, message ) = match
            if (wrongAnswers.any { it.matches(message) }) {
                Scheduler.scheduleTask {
                    getChest(name, false)?.let {
                        if (answers.contains(it)) return@scheduleTask
                        answers.add(it)
                    }
                }
                return@on
            }
            if (!solutions.any { it.matches(message) }) return@on

            event.cancel()
            ChatUtils.sendMessage("&e[NPC] &b&l$name: &a&l$message")
            Scheduler.scheduleTask {
                getChest(name, true)?.let {
                    if (answers.contains(it)) return@scheduleTask
                    answers.add(it)
                }
            }
        }

        on<NameChangeEvent> { event ->
            entityList[event.name] = event.entityId
        }

        on<RenderWorldEvent> {
            if (answers.isEmpty()) return@on
            answers.forEach {
                val entityPos = it.entityPos
                val chestPos = it.chestPos
                val isCorrect = it.isCorrect

                // Render chest highlight
                Context.Immediate?.renderBox(
                    chestPos.first, 69.0, chestPos.second,
                    color = if (isCorrect) CORRECT_OUTLINE_COLOR else WRONG_OUTLINE_COLOR,
                    true
                )
                Context.Immediate?.renderFilledBox(
                    chestPos.first, 69.0, chestPos.second,
                    color = if (isCorrect) CORRECT_FILLED_COLOR else WRONG_FILLED_COLOR
                )

                // Render entity highlight
                Context.Immediate?.renderBox(
                    entityPos.x - 0.5, entityPos.y, entityPos.z - 0.5,
                    0.8, 2.0,
                    if (isCorrect) CORRECT_OUTLINE_COLOR else WRONG_OUTLINE_COLOR,
                    true
                )
                Context.Immediate?.renderFilledBox(
                    entityPos.x - 0.5, entityPos.y, entityPos.z - 0.5,
                    0.8, 2.0,
                    if (isCorrect) CORRECT_FILLED_COLOR else WRONG_FILLED_COLOR
                )
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        reset()
    }

    private fun reset() {
        enteredAt = -1
        answers.clear()
        entityList.clear()
    }

    private fun getChest(name: String, isCorrect: Boolean): AnswerData? {
        val entityId = entityList[name] ?: return null
        val entity = minecraft.level?.getEntity(entityId) ?: return null
        val pos = entity.position() ?: return null
        val x0 = pos.x - 0.5
        val z0 = pos.z - 0.5
        var chestPos: Pair<Double, Double>? = null

        for (dir in dirs) {
            val ( dx, dz ) = dir
            val blockAt = WorldUtils.getBlockState(x0 + dx, 69.0, z0 + dz) ?: continue
            if (blockAt.block != Blocks.CHEST) continue
            chestPos = x0 + dx to z0 + dz
        }
        if (chestPos == null) return null

        return AnswerData(pos, chestPos, isCorrect)
    }
}