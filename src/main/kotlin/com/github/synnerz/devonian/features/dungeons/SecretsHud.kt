package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ActionbarEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils.colorCodes

object SecretsHud : TextHudFeature(
    "secretsHud",
    "Shows the current room's secrets data",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val secretsRegex = ".* (\\d+)/(\\d+) Secrets".toRegex()
    private val formattedSecretsRegex = "(§7\\d+/\\d+ Secrets)".toRegex()

    override fun initialize() {
        on<ActionbarEvent> { event ->
            val match = event.matches(secretsRegex) ?: return@on
            ChatUtils.sendActionbar(event.text.colorCodes().replace(formattedSecretsRegex, ""))
            val current = match[0].toIntOrNull() ?: return@on
            val maximum = match[1].toIntOrNull() ?: return@on
            val format = if (current == maximum) "&f" else "&7"
            setLine("${format}${current}/&f${maximum}")
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&70/&f7")

    override fun onWorldChange(event: WorldChangeEvent) {
        clearLines()
    }
}