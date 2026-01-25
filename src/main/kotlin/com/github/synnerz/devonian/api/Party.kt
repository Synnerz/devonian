package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EventBus
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket.PartyRole
import java.util.*

object Party {
    // https://regex101.com/r/NYnC2x/1
    private val partyRegexes = listOf(
        "^(?:\\[[^ ]+] )?(\\w{1,16}) has promoted (?:\\[[^ ]+] )?(\\w{1,16}) to Party (?:Leader|Moderator)$".toRegex(),
        "^You left the party\\.$".toRegex(),
        "^(?:\\[[^ ]+] )?(\\w{1,16}) has disbanded the party!$".toRegex(),
        "^The party was disbanded because (?:all invites expired and the party was empty|the party leader disconnected)\\.$".toRegex(),
        "^You have been kicked from the party by (?:\\[[^ ]+] )?(\\w{1,16})$".toRegex(),
        "^You are not this party's leader!$".toRegex(),
        "^The party was transferred to (?:\\[[^ ]+] )?(\\w{1,16}) (?:by|because) (?:\\[[^ ]+] )?(\\w{1,16})(?: left)?$".toRegex(),
        "^Kicked (?:\\[[^ ]+] )?(\\w{1,16}) because they were offline\\.$".toRegex(),
        "^(?:\\[[^ ]+] )?(\\w{1,16}) was removed from your party because they disconnected\\.$".toRegex(),
        "^(?:\\[[^ ]+] )?(\\w{1,16}) has (?:been removed from the party|left the party)\\.$".toRegex(),
        "^You have joined (?:\\[[^ ]+] )?(\\w{1,16})'s party!$".toRegex(),
        "^Party Finder > (?:\\[[^ ]+] )?(\\w{1,16}) joined the .*$".toRegex(),
    )
    var inParty = false
    var members: Map<UUID, PartyRole> = mapOf()
    var isLeader = false
    private var lastRequest = -1L

    fun initialize() {
        EventBus.on<ChatEvent> { event ->
            if (!partyRegexes.any { it.matches(event.message) }) return@on
            // TODO: maybe it'll be useful to queue this to run after the 2s ?
            if (lastRequest != -1L && System.currentTimeMillis() - lastRequest <= 2000) return@on

            lastRequest = System.currentTimeMillis()
            HypixelModApi.requestPartyInfo()
        }

        EventBus.on<HypixelModApi.PartyInfoPacket> { event ->
            if (!event.inParty) {
                inParty = false
                members = mapOf()
                isLeader = false
                return@on
            }

            inParty = true
            members = event.members
            isLeader = event.isLeader
        }
    }
}