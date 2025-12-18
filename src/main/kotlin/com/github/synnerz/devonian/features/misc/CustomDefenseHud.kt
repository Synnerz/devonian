package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ActionbarEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes

object CustomDefenseHud : TextHudFeature(
    "customDefenseHud",
    "Allows you to move the defense that appears in action bar (above your hotbar)",
    subcategory = "General"
) {
    private val SETTING_ONLY_HIDE = addSwitch(
        "onlyHide",
        false,
        "Hides the action bar defense as well as the custom hud",
        "Only Hide Defense"
    )
    private val defenseRegex = ".* ([\\d,]+)❈ Defense .*".toRegex()
    private val formattedDefenseRegex = " *§a[\\d,]+§a❈ Defense *".toRegex()

    override fun initialize() {
        on<ActionbarEvent> { event ->
            val match = event.matches(defenseRegex) ?: return@on
            event.cancel()
            ChatUtils.sendActionbar(event.text.colorCodes().replace(formattedDefenseRegex, "     "))
            if (SETTING_ONLY_HIDE.get()) return@on
            Scheduler.scheduleTask {
                setLine("&a${match[0]}❈")
            }
        }

        on<RenderOverlayEvent> {
            if (SETTING_ONLY_HIDE.get()) return@on
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&a500❈")

    override fun onWorldChange(event: WorldChangeEvent) {
        clearLines()
    }
}