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
    private const val SETTING_SHOW_SECRETS = true
    private const val SETTING_SHOW_MIMIC_PRINCE = true

    private fun getLines(
        score: Int,
        mimic: Boolean,
        prince: Boolean,
        secrets: Int,
        secretsRequired: Int,
        totalSecrets: Int,
        floor: FloorType
    ): List<String> {
        val tier = when {
            score < 100 -> "&cD"
            score < 160 -> "&9C"
            score < 230 -> "&aB"
            score < 270 -> "&5A"
            score < 300 -> "&eS"
            else -> "&6&lS+"
        }
        val arr = mutableListOf(
            "&eScore: ${
                when {
                    score < 270 -> "&c"
                    score < 300 -> "&e"
                    else -> "&a"
                }
            }$score &7(${tier}&r&7)"
        )
        if (SETTING_SHOW_SECRETS) arr.add(
            "&eSecrets: ${if (secrets >= secretsRequired) "&a" else "&c"}${secrets}&7/&a${secretsRequired}" +
            (if (floor.requiredPercent == 1.0) "" else " &7(&6Total: $totalSecrets&7)")
        )
        if (SETTING_SHOW_MIMIC_PRINCE) {
            if (floor.floorNum >= 6) arr.add("&eMimic: ${if (mimic) "&a&l✔" else "&c&l✘"}")
            arr.add("&ePrince: ${if (prince) "&a&l✔" else "&c&l✘"}")
        }
        return arr
    }

    override fun getEditText(): List<String> = getLines(
        301,
        false, true,
        54, 61, 61,
        FloorType.M7
    )

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            setLines(
                getLines(
                    Dungeons.score.value,
                    Dungeons.mimicKilled.value,
                    Dungeons.princeKilled.value,
                    Dungeons.secretsFound.value,
                    Dungeons.totalSecretsRequired.value,
                    Dungeons.totalSecrets.value,
                    Dungeons.floor
                )
            )
            draw(event.ctx)
        }
    }
}