package com.github.synnerz.devonian.features.slayers

import com.github.synnerz.devonian.events.ChatEvent
import com.github.synnerz.devonian.events.ScoreboardEvent
import com.github.synnerz.devonian.events.ServerTickEvent
import com.github.synnerz.devonian.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.ChatUtils

object BossSlainTime : Feature("bossSlainTime") {
    private val questStartedRegex = "^Slay the boss!$".toRegex()
    private val questCompletedRegex = "^Boss slain!$".toRegex()
    private val questCompletedChatRegex = "^  SLAYER QUEST COMPLETE!$".toRegex()
    private var serverTicks = 0
    private var spawnedAtTicks = 0
    private var spawnedAtTime = 0L
    private var killedAtTicks = 0
    private var killedAtTime = 0L

    override fun initialize() {
        on<ServerTickEvent> {
            serverTicks++
        }

        on<ScoreboardEvent> { event ->
            val questStarted = event.matches(questStartedRegex)
            if (questStarted != null) {
                spawnedAtTicks = serverTicks
                spawnedAtTime = System.currentTimeMillis()
                return@on
            }
            if (event.matches(questCompletedRegex) == null) return@on

            killedAtTicks = serverTicks
            killedAtTime = System.currentTimeMillis()
        }

        on<ChatEvent> { event ->
            if (event.matches(questCompletedChatRegex) == null) return@on
            if (killedAtTicks == 0) killedAtTicks = serverTicks
            if (killedAtTime == 0L) killedAtTime = System.currentTimeMillis()

            val realTime = (killedAtTime - spawnedAtTime) / 1000
            val serverTime = (killedAtTicks - spawnedAtTicks) * 0.05

            // TODO: making this onDeath based might be better for accuracy
            ChatUtils.sendMessage("&aBoss Took&f: &b${"%.2f".format(realTime.toFloat())}s &7- &b${"%.2f".format(serverTime.toFloat())}s", true)
            reset()
        }

        on<WorldChangeEvent> {
            reset()
        }
    }

    private fun reset() {
        serverTicks = 0
        spawnedAtTicks = 0
        spawnedAtTime = 0
        killedAtTicks = 0
        killedAtTime = 0
    }
}