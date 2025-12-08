package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.FloorType
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object ScoreDisplay : TextHudFeature(
    "dungeonScoreDisplay",
    "Displays score information.",
    "Dungeons",
    "catacombs"
) {
    private val SETTING_ILLEGALMAP_FORMAT = addSwitch(
        "illegapStyle",
        "§4OVERRIDES OTHER SETTINGS",
        "IllegalMap Score Style",
        false
    )
    private val SETTING_SHOW_SECRETS = addSwitch(
        "showSecrets",
        "",
        "Show Secrets",
        true
    )
    private val SETTING_SHOW_MIMIC_PRINCE = addSwitch(
        "showMimicPrince",
        "",
        "Show Mimic/Prince State",
        true
    )

    private fun getLines(
        score: Int,
        mimic: Boolean,
        prince: Boolean,
        crypts: Int,
        secrets: Int,
        secretsRequired: Int,
        totalSecrets: Int,
        remainingSecrets: Int,
        floor: FloorType
    ): List<String> {
        val tierColor = when {
            score < 100 -> "&c"
            score < 160 -> "&9"
            score < 230 -> "&a"
            score < 270 -> "&5"
            score < 300 -> "&e"
            else -> "&6&l"
        }
        val m = if (mimic) "&a&l✔" else "&c&l✘"
        val p = if (prince) "&a&l✔" else "&c&l✘"
        if (SETTING_ILLEGALMAP_FORMAT.get()) return listOf(
            "&7Secrets: &b$secrets&7-&e${remainingSecrets}&7-&c$totalSecrets &8| &7Score: $tierColor$score",
            "&7Crypts: ${if (crypts >= 5) "&a" else "&c"}$crypts &8| &7M: $m &8| &7P: $p"
        )
        val tier = when {
            score < 100 -> "D"
            score < 160 -> "C"
            score < 230 -> "B"
            score < 270 -> "A"
            score < 300 -> "S"
            else -> "S+"
        }
        val arr = mutableListOf(
            "&eScore: ${
                when {
                    score < 270 -> "&c"
                    score < 300 -> "&e"
                    else -> "&a"
                }
            }$score &7($tierColor$tier&r&7)"
        )
        if (SETTING_SHOW_SECRETS.get()) arr.add(
            "&eSecrets: ${if (secrets >= secretsRequired) "&a" else "&c"}$secrets&7/&a$secretsRequired" +
            (if (floor.requiredPercent == 1.0) "" else " &7(&6Total: $totalSecrets&7)")
        )
        if (SETTING_SHOW_MIMIC_PRINCE.get()) {
            if (floor.floorNum >= 6) arr.add("&eMimic: $m")
            arr.add("&ePrince: $p")
        }
        return arr
    }

    override fun getEditText(): List<String> = getLines(
        301,
        false, true, 4,
        54, 61, 61,
        0,
        FloorType.M7
    )

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            if (SETTING_ILLEGALMAP_FORMAT.get() && Dungeons.inBoss.value) return@on

            setLines(
                getLines(
                    Dungeons.score.value,
                    Dungeons.mimicKilled.value,
                    Dungeons.princeKilled.value,
                    Dungeons.crypts.value,
                    Dungeons.secretsFound.value,
                    Dungeons.totalSecretsRequired.value,
                    Dungeons.totalSecrets.value,
                    Dungeons.remainingMinSecrets.value,
                    Dungeons.floor
                )
            )
            draw(event.ctx)
        }
    }
}