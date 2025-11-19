package com.github.synnerz.devonian.features.end

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import kotlin.math.min

object GolemLootQuality : Feature(
    "golemLootQuality",
    "Shows your loot quality for the Golem and whether you could roll for a Tier Booster Core/Legendary Golem Pet/Epic Golem Pet",
    "end",
    "the end"
) {
    private val positionQuality = listOf(
        200,
        175,
        150,
        125,
        110,
        100,
        100,
        100,
        90,
        90,
        80,
        80
    )
    private val golemKilledRegex = "^ *END STONE PROTECTOR DOWN!$".toRegex()
    private val yourDamageRegex = "^ *Your Damage: ([\\d,]+)(?: \\(NEW RECORD!\\))? \\(Position #(\\d+)\\)\$".toRegex()
    private val firstDamageRegex = "^ *1st Damager - .* - ([\\d,]+)$".toRegex()
    private val zealotKillsRegex = "^ *Zealots Contributed: (\\d+)/100$".toRegex()
    var golemKilled = false
    var firstDamage = 1
    var yourDamage = 1
    var yourPosition = 1

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(golemKilledRegex) != null) {
                golemKilled = true
                return@on
            }
            if (!golemKilled) return@on

            val topDamager = event.matches(firstDamageRegex)
            if (topDamager != null) {
                firstDamage = topDamager[0]
                    .replace(",", "")
                    .replace(".", "")
                    .toInt()
                    .coerceAtLeast(1)
                return@on
            }

            val yourDamageMatch = event.matches(yourDamageRegex)
            if (yourDamageMatch != null) {
                yourDamage = yourDamageMatch[0]
                    .replace(",", "")
                    .replace(".", "")
                    .toInt()
                    .coerceAtLeast(1)
                yourPosition = yourDamageMatch[1].toInt()
                return@on
            }

            val match = event.matches(zealotKillsRegex) ?: return@on
            val zealotKills = match[0].toInt()
            val placementQuality = positionQuality[yourPosition - 1]
            val quality = placementQuality + (50 * yourDamage / firstDamage) + min(zealotKills, 100)

            val tbc = if (quality >= 250) "&a\uD83D\uDDF8" else "&cx"
            val legPet = if (quality >= 235) "&a\uD83D\uDDF8" else "&cx"
            val epicPet = if (quality >= 220) "&a\uD83D\uDDF8" else "&cx"

            Scheduler.scheduleServerTask(20) {
                ChatUtils.sendMessage("&bGolem loot quality &6$quality &cTBC &b[$tbc&b] | &6LegPet &b[$legPet&b] | &5EpicPet &b[$epicPet&b]", true)
            }

            golemKilled = false
            firstDamage = 1
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        golemKilled = false
        firstDamage = 1
        yourDamage = 1
        yourPosition = 1
    }
}