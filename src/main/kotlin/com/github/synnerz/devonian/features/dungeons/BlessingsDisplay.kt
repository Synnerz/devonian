package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabFooterEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils
import java.util.*

object BlessingsDisplay : TextHudFeature(
    "blessingsDisplay",
    "Displays the blessings in the current dungeon run",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    searchTags = setOf("power"),
) {
    private val SETTING_ROMAN = addSwitch(
        "roman",
        false,
        "",
        "Use Roman Numerals",
    )

    private val blessingRegex = "^Blessing of (\\w+) ([IVX]+)$".toRegex()
    private val blessings = linkedMapOf<String, Int>()

    private fun clear() {
        blessings.clear()
        blessings["Power"] = 0
        blessings["Time"] = 0
        blessings["Wisdom"] = 0
        blessings["Stone"] = 0
        blessings["Life"] = 0
    }

    init {
        clear()
    }

    private fun format(values: SequencedMap<String, Int>): List<String> {
        return values.mapNotNull {
            if (it.value == 0) return@mapNotNull null

            val format = when (it.key) {
                "Life" -> "&2"
                "Power" -> "&4"
                "Stone" -> "&7"
                "Wisdom" -> "&b"
                "Time" -> "&6"
                else -> "&e"
            }

            if (SETTING_ROMAN.get()) "$format${it.key} &a${StringUtils.formatRoman(it.value)}"
            else "$format${it.key}&f: &a${it.value}"
        }
    }

    override fun initialize() {
        on<TabFooterEvent> { event ->
            val match = event.matches(blessingRegex) ?: return@on
            val (type, roman) = match
            val number = StringUtils.parseRoman(roman)

            Scheduler.scheduleTask {
                blessings[type] = number
            }
        }

        on<ClientThreadServerTickEvent> {
            setLines(format(blessings))
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        clear()
    }

    override fun getEditText(): List<String> = format(
        linkedMapOf(
            "Power" to 29,
            "Time" to 5,
            "Wisdom" to 11,
            "Stone" to 10,
            "Life" to 36,
        )
    )
}