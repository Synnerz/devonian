package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object CroesusChestCounter : TextHudFeature(
    "croesusChestCounter",
    "Displays the amount of chests in croesus",
    Categories.DUNGEONS,
    subcategory = "HUD",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Location.stateArea.map { it == "dungeon hub" || it == "catacombs" })
    }

    private val SETTING_ALERT_FULL = addSwitch(
        "alertWhenFull",
        true,
        "Alerts you whenever your chests are full (note this may not be accurate if your full team dies, the counter itself should though)",
        "Alert When Full"
    )
    private val SETTING_ALERT_SOUND = addSwitch(
        "alertSound",
        true,
        "Plays a sound whenever the alert shows",
        "Alert Sound"
    )
    private val chestCountRegex = "^ Unclaimed chests: (\\d+)$".toRegex()
    private val teamScoreRegex = "^ *Team Score: (\\d+) \\((.{1,2})\\)(?: \\(NEW RECORD!\\))?$".toRegex()
    private val squadWipeRegex = "^Warning! The instance will close in 10s\\.$".toRegex()
    private const val MAX_CHESTS = 5
    private var chests = 0
        set(value) {
            field = value.coerceIn(0..MAX_CHESTS)
        }
    private var added = false
    private var alerted = false

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val match = event.matches(chestCountRegex) ?: return@on
            val amount = match[0].toIntOrNull() ?: return@on

            if (chests >= MAX_CHESTS && amount < MAX_CHESTS && alerted)
                alerted = false
            chests = amount
        }

        on<ChatEvent> { event ->
            // predicting whether the user got a chest was incorrect
            if (added && event.matches(squadWipeRegex) != null) {
                chests--
                added = false
                alerted = false
                return@on
            }
            if (event.matches(teamScoreRegex) == null || added) return@on

            added = true
            chests++
        }

        on<ClientThreadServerTickEvent> {
            if (SETTING_ALERT_FULL.get() && !alerted && chests >= MAX_CHESTS) {
                Alert.show("&cCroesus Chest Full", 3000, SETTING_ALERT_SOUND.get())
                alerted = true
            }
            setLine("${colorForNumberReverse(chests, MAX_CHESTS)}$chests&f/&4${MAX_CHESTS}")
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        added = false
    }

    override fun getEditText(): List<String> = listOf("&a5&f/&460")

    private fun colorForNumberReverse(num: Int, max: Int) = when {
        num >= max * 0.75 -> "&4"
        num >= max * 0.50 -> "&c"
        num >= max * 0.25 -> "&e"
        else -> "&a"
    }
}