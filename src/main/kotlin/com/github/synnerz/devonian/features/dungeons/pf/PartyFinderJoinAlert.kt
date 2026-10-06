package com.github.synnerz.devonian.features.dungeons.pf

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert

object PartyFinderJoinAlert : Feature(
    "partyFinderJoinAlert",
    "Sends an alert when a player joins the party.",
    Categories.PARTY_FINDER,
    subcategory = "General",
) {
    private val SETTING_ONLY_FULL = addSwitch(
        "onlyFull",
        false,
        "Only sends the alert when the party is full, rather than per player joining.",
        "Only Alert Full",
    )

    private val partyFinderJoinRegex = "^Party Finder > (\\w{1,16}) joined the dungeon group! \\((?:Healer|Tank|Mage|Berserk|Archer) Level \\d+\\)$".toRegex()

    private var wasLastJoined = false

    override fun initialize() {
        on<ChatEvent> { event ->
            if (
                SETTING_ONLY_FULL.get() &&
                event.message == "Party Finder > Your dungeon group is full! Click here to warp to the dungeon!"
            ) {
                if (!wasLastJoined) Alert.show("Party Full")
                return@on
            }

            val (name) = event.matches(partyFinderJoinRegex) ?: return@on
            wasLastJoined = name.equals(minecraft.player?.name?.string, ignoreCase = true)
            if (!wasLastJoined && !SETTING_ONLY_FULL.get()) Alert.show("$name joined")
        }
    }
}