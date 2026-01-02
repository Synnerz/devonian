package com.github.synnerz.devonian.features.garden

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetContentEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.world.item.Items

object GrowthStageTimer : TextHudFeature(
    "growthStageTimer",
    "shows time until next growth stage",
    Categories.GARDEN,
) {
    private val stageRegex = "^Next Stage: ([\\dhms ]+)$".toRegex()

    private var finishTime = -1L
    private var cropId = -1

    private fun formatTime(secs: Int): String {
        if (secs < 0) return "&cOpen Crop Diagnostics"
        return StringUtils.colorForNumber(secs, 300) +
            "\uD83C\uDF33" +
            StringUtils.formatTime(secs * 1000L, 0)
    }

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            cropId = if (event.titleStr == "Crop Diagnostics") event.containerId else -1
        }

        on<ServerContainerSetContentEvent> { event ->
            if (event.containerId != cropId) return@on

            val tree = event.items.getOrNull(20) ?: return@on
            if (tree.isEmpty) return@on

            if (tree.item != Items.JUNGLE_SAPLING) return@on

            val lore = ItemUtils.lore(tree) ?: return@on
            lore.forEach {
                val match = stageRegex.matchEntire(it) ?: return@forEach
                val timer = match.groupValues.getOrNull(1) ?: return@on
                finishTime = System.currentTimeMillis() + StringUtils.parseTimer(timer) * 1000L
            }
        }

        on<RenderOverlayEvent> { event ->
            if (finishTime < 0L) setLine(formatTime(-1))
            else setLine(formatTime(((finishTime - System.currentTimeMillis()) / 1000L).toInt()))

            draw(event.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        cropId = -1
    }

    override fun getEditText(): List<String> = listOf(formatTime(6861))
}