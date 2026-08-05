package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.ModifyChatEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.network.chat.Component
import java.awt.Color

object TriviaSolver : Feature(
    "triviaSolver",
    "Highlights the correct answer block and chat message for quiz puzzle.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
    searchTags = setOf("quiz", "puzzle"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val solutions = mapOf(
        "What is the status of The Watcher?" to listOf("Stalker"),
        "What is the status of Bonzo?" to listOf("New Necromancer"),
        "What is the status of Scarf?" to listOf("Apprentice Necromancer"),
        "What is the status of The Professor?" to listOf("Professor"),
        "What is the status of Thorn?" to listOf("Shaman Necromancer"),
        "What is the status of Livid?" to listOf("Master Necromancer"),
        "What is the status of Sadan?" to listOf("Necromancer Lord"),
        "What is the status of Maxor, Storm, Goldor, and Necron?" to listOf("The Wither Lords"),
        "How many total Fairy Souls are there?" to listOf("289 Fairy Souls"),
        "How many Fairy Souls are there in Spider's Den?" to listOf("19 Fairy Souls"),
        "How many Fairy Souls are there in Spiders Den?" to listOf("19 Fairy Souls"),
        "How many Fairy Souls are there in The End?" to listOf("12 Fairy Souls"),
        "How many Fairy Souls are there in The Farming Islands?" to listOf("20 Fairy Souls"),
        "How many Fairy Souls are there in Crimson Isle?" to listOf("29 Fairy Souls"),
        "How many Fairy Souls are there in The Park?" to listOf("12 Fairy Souls"),
        "How many Fairy Souls are there in Jerry's Workshop?" to listOf("5 Fairy Souls"),
        "How many Fairy Souls are there in Hub?" to listOf("80 Fairy Souls"),
        "How many Fairy Souls are there in The Hub?" to listOf("80 Fairy Souls"),
        "How many Fairy Souls are there in Deep Caverns?" to listOf("21 Fairy Souls"),
        "How many Fairy Souls are there in Gold Mine?" to listOf("12 Fairy Souls"),
        "How many Fairy Souls are there in Dungeon Hub?" to listOf("7 Fairy Souls"),
        "Which brother is on the Spider's Den?" to listOf("Rick"),
        "Which brother is on the Spiders Den?" to listOf("Rick"),
        "What is the name of Rick's brother?" to listOf("Pat"),
        "What is the name of the vendor in the Hub who sells stained glass?" to listOf("Wool Weaver"),
        "What is the name of the person that upgrades pets?" to listOf("Kat"),
        "What is the name of the lady of the Nether?" to listOf("Elle"),
        "Which villager in the Village gives you a Rogue Sword?" to listOf("Jamie"),
        "How many unique minions are there?" to listOf("61 Minions"),
        "Which of these enemies does not spawn in the Spider's Den?" to listOf(
            "Zombie Spider",
            "Cave Spider",
            "Wither Skeleton",
            "Dashing Spooder",
            "Broodfather",
            "Night Spider"
        ),
        "Which of these enemies does not spawn in the Spiders Den?" to listOf(
            "Zombie Spider",
            "Cave Spider",
            "Wither Skeleton",
            "Dashing Spooder",
            "Broodfather",
            "Night Spider"
        ),
        "Which of these monsters only spawns at night?" to listOf(
            "Zombie Villager",
            "Ghast"
        ),
        "Which of these is not a dragon in The End?" to listOf(
            "Zoomer Dragon",
            "Weak Dragon",
            "Stonk Dragon",
            "Holy Dragon",
            "Boomer Dragon",
            "Booger Dragon",
            "Older Dragon",
            "Elder Dragon",
            "Stable Dragon",
            "Professor Dragon"
        )
    )
    private val typeBlocks = mapOf(
        "ⓐ" to (20 to 6),
        "ⓑ" to (15 to 9),
        "ⓒ" to (10 to 6),
    )
    var inQuiz = false
    var quizRoom: DungeonRoom? = null
    var solution: List<String>? = null
    var currentAnswer: String? = null
    var enteredAt = -1

    override fun initialize() {
        on<DungeonEvent.RoomEnter> { event ->
            val room = event.room
            inQuiz = if (room.name == "Quiz") {
                inQuiz = true
                quizRoom = room
                true
            } else room.doors.any {
                it.rooms.any {
                    if (it.name == "Quiz") {
                        quizRoom = it
                        true
                    } else false
                }
            }
        }

        on<DungeonEvent.RoomLeave> {
            if (!inQuiz) return@on
            inQuiz = false
        }

        on<ModifyChatEvent> { event ->
            event.matches("^ *([ⓐⓑⓒ]) (.*)$".toRegex())?.let {
                if (solution == null) return@on
                val ( type, msg ) = it

                if (!solution!!.contains(msg)) {
                    event.overrideValue = Component.literal("    §c$type $msg")
                } else {
                    currentAnswer = type
                    event.overrideValue = Component.literal("    §a$type $msg")
                }

                return@on
            }

            event.matches("^\\[STATUE] Oruo the Omniscient: I am Oruo the Omniscient\\. I have lived many lives\\. I have learned all there is to know\\.$".toRegex())?.let {
                enteredAt = EventBus.serverTicks()
                return@on
            }

            event.matches("^\\[STATUE] Oruo the Omniscient: \\w+ answered Question #\\d+ correctly!$".toRegex())?.let {
                resetSolution()
                return@on
            }

            event.matches("^\\[STATUE] Oruo the Omniscient: \\w+ answered the final question correctly!$".toRegex())?.let {
                if (!PuzzleTimers.isEnabled()) return@on
                val time = (EventBus.serverTicks() - enteredAt) * 0.05
                val seconds = "%.2fs".format(time)
                ChatUtils.sendMessage("&bQuiz took&f: &6$seconds", true)
                resetSolution()
                return@on
            }

            event.matches("^\\[STATUE] Oruo the Omniscient: Yikes$".toRegex())?.let {
                resetSolution()
                return@on
            }

            val match = event.matches("^ *(.*\\?)$".toRegex()) ?: return@on
            var question = match[0]

            if (question == "What SkyBlock year is it?") {
                solution = listOf(currentYear())
                return@on
            }

            if (question.trim() == "glass?")
                question = "What is the name of the vendor in the Hub who sells stained glass?"

            val solutionData = solutions[question] ?: return@on

            solution = solutionData
        }

        on<RenderWorldEvent> {
            if (!inQuiz || solution == null || currentAnswer == null) return@on
            val pos = typeBlocks[currentAnswer] ?: return@on
            // Should never happen but just in case
            val room = quizRoom ?: return@on
            val realPos = room.fromComp(pos.first, pos.second) ?: return@on
            Render3DImmediate.renderWireframeBox(
                realPos.first.toDouble(), 70.0, realPos.second.toDouble(),
                1.0, 1.0,
                Color.GREEN,
                phase = true,
            )
            Render3DImmediate.renderFilledBox(
                realPos.first.toDouble(), 70.0, realPos.second.toDouble(),
                1.0, 1.0,
                Color(0, 255, 0, 80),
                phase = false,
            )
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inQuiz = false
        quizRoom = null
        resetSolution()
    }

    private fun resetSolution() {
        enteredAt = -1
        solution = null
        currentAnswer = null
    }

    private fun currentYear(): String {
        val year = (((System.currentTimeMillis() / 1000) - 1560276000) / 446400) + 1
        return "Year $year"
    }
}