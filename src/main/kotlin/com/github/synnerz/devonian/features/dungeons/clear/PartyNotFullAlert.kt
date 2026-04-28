package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Party
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert

object PartyNotFullAlert : Feature(
    "partyNotFullAlert",
    "Shows an alert whenever the current run player count is not the same as party count",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Alerts",
) {
    private val SETTING_PLAY_SOUND = addSwitch(
        "playSound",
        true,
        "Plays a sound whenever the alert is shown",
        "Play Sound"
    )
    private val playerListRegex = "^ *Party \\((\\d+)\\)$".toRegex()
    private val playerReadyRegex = "^\\w{1,16} is now ready!$".toRegex()
    private var sent = false
    private var members = -1

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val ( count ) = event.matches(playerListRegex) ?: return@on
            members = count.toIntOrNull() ?: 1
            sent = false
        }

        on<ChatEvent> { event ->
            if (sent) return@on
            if (event.matches(playerReadyRegex) == null) return@on
            if (!Party.inParty || Party.members.size == members) return@on

            Alert.show("&cParty is not full (${members}/${Party.members.size})", 2000, SETTING_PLAY_SOUND.get())
            ChatUtils.sendMessage("&cParty is not full (${members}/${Party.members.size})", true)
            sent = true
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        members = -1
        sent = false
    }
}