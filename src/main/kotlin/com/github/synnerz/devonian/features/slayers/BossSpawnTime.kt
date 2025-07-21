package com.github.synnerz.devonian.features.slayers

import com.github.synnerz.devonian.events.ChatEvent
import com.github.synnerz.devonian.events.ScoreboardEvent
import com.github.synnerz.devonian.events.ServerTickEvent
import com.github.synnerz.devonian.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.ChatUtils

object BossSpawnTime : Feature("bossSpawnTime") {
    private val questStartedRegex = "^  SLAYER QUEST STARTED!$".toRegex()
    private val bossSpawnedRegex = "^Slay the boss!".toRegex()
    private var serverTicks = 0
    private var startedAtTicks = 0
    private var startedAtTime = 0L

    override fun initialize() {
        on<ServerTickEvent> {
            serverTicks++
        }

        on<ChatEvent> { event ->
            if (event.matches(questStartedRegex) == null) return@on
            startedAtTicks = serverTicks
            startedAtTime = System.currentTimeMillis()
        }

        on<ScoreboardEvent> { event ->
            if (event.matches(bossSpawnedRegex) == null || startedAtTime == 0L || startedAtTicks == 0) return@on

            val time = (System.currentTimeMillis() - startedAtTime) / 1000
            val ticks = (serverTicks - startedAtTicks) * 0.05

            ChatUtils.sendMessage("&aBoss Spawn Took&f: &b${"%.2fs".format(time.toFloat())} &7- &b${"%.2fs".format(ticks.toFloat())}", true)
            reset()
        }

        on<WorldChangeEvent> {
            reset()
        }
    }

    private fun reset() {
        serverTicks = 0
        startedAtTicks = 0
        startedAtTime = 0
    }
}