package com.github.synnerz.devonian.features.end

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.WorldFeature

object GolemDPS : WorldFeature("golemDps", "the end") {
    private val golemSpawnRegex = "^BEWARE - An End Stone Protector has risen!$".toRegex()
    private val golemKilledRegex = "^ *END STONE PROTECTOR DOWN!$".toRegex()
    private val yourDamageRegex = "^ *Your Damage: ([\\d,]+)(?: \\(NEW RECORD!\\))? \\(Position #\\d+\\)\$".toRegex()
    var spawnAt = -1L
    var killedAt = -1L

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(golemSpawnRegex) ?: return@on

            // TODO: use server ticks later on
            spawnAt = System.currentTimeMillis()
        }

        on<ChatEvent> { event ->
            val golemKilled = event.matches(golemKilledRegex)
            if (golemKilled != null) {
                killedAt = System.currentTimeMillis()
                return@on
            }
            if (killedAt == -1L) return@on

            val match = event.matches(yourDamageRegex) ?: return@on
            val yourDamage = match[0]
                .replace(",", "")
                .replace(".", "")
                .toFloat()
                .coerceAtLeast(1f)
            val timeToKill = killedAt - spawnAt
            val dps = "%.2f".format(yourDamage / timeToKill)
            val seconds = "%.2fs".format(timeToKill / 1000f)

            killedAt = -1L
            spawnAt = -1L

            // Delay it so it's sent after the leaderboard message
            Scheduler.scheduleTask(20) {
                ChatUtils.sendMessage("&bYour Golem DPS was &6${dps} &bin &6${seconds}", true)
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        spawnAt = -1L
        killedAt = -1L
    }
}