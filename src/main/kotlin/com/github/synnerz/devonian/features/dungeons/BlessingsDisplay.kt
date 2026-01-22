package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabFooterEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils

object BlessingsDisplay : TextHudFeature(
    "blessingsDisplay",
    "Displays the blessings in the current dungeon run",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD"
) {
    private val blessingRegex = "^Blessing of (\\w+) ([IVX]+)$".toRegex()
    private val blessings = mutableMapOf<String, Int>()

    override fun initialize() {
        on<TabFooterEvent> { event ->
            val match = event.matches(blessingRegex) ?: return@on
            val ( type, roman ) = match
            val number = StringUtils.parseRoman(roman)
            val format = when (type) {
                "Life" -> "&2"
                "Power" -> "&4"
                "Stone" -> "&7"
                "Wisdom" -> "&b"
                "Time" -> "&6"
                else -> "&e"
            }

            Scheduler.scheduleTask {
                blessings["$format$type"] = number
            }
        }

        on<ClientThreadServerTickEvent> {
            setLines(blessings.entries.map { (k, v) ->
                "$k&f: &a$v"
            })
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        blessings.clear()
    }

    override fun getEditText(): List<String> = listOf("&2Life&f: &a5", "&4Power&f: &a5", "&7Stone&f: &a5", "&bWisdom&f: &a5", "&6Time&f: &a2")
}