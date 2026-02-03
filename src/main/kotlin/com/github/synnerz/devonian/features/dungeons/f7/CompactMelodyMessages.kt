package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatChannelEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.misc.chat.CompactChatComponent
import com.github.synnerz.devonian.features.misc.chat.MutePartySpam
import net.minecraft.network.chat.Component

object CompactMelodyMessages : Feature(
    "compactMelodyMessage",
    "Makes party chat spam more bearable.",
    Categories.DUNGEONS,
    subcategory = "F7",
) {
    private val SETTING_MUTE = addSwitch(
        "mute",
        true,
        "",
        "Mute Melody Messages",
    )

    private val fixedMelodies = listOf(
        listOf("melody"),
        listOf("Melody Terminal start"),
        listOf("Melody Terminal Start"),
        listOf(
            "Melody terminal is at 25%",
            "Melody terminal is at 50%",
            "Melody terminal is at 75%",
        ),
        listOf(
            "Melody ♪ Terminal [1/4]!",
            "Melody ♪ Terminal [2/4]!",
            "Melody ♪ Terminal [3/4]!",
        ),
        listOf(
            "Melody 25%",
            "Melody 50%",
            "Melody 75%",
        ),
    )
    private val melodyFracRegex = "\\b([0-3])/4\\b".toRegex()
    private val melodyPercRegex = "\\b([27]5|[50]?0)%(?=[\\s\\W]|$)".toRegex()

    private fun isMelodyMessage(msg: String): Boolean {
        return melodyFracRegex.containsMatchIn(msg) || melodyPercRegex.containsMatchIn(msg)
    }

    private val lastMessages = mutableMapOf<String, String>()
    private val knownMelodyMessages = mutableMapOf<String, Boolean>().also { map ->
        fixedMelodies.flatten().forEach {
            map[it] = true
        }
    }
    private val prevMelodyMessage = mutableMapOf<String, Component>()

    override fun initialize() {
        on<ChatChannelEvent.PartyChatEvent> { event ->
            if (knownMelodyMessages.getOrPut(event.userMessage) { isMelodyMessage(event.userMessage) }) {
                lastMessages.computeIfPresent(event.name) { _, msg ->
                    knownMelodyMessages[msg] = true
                    return@computeIfPresent null
                }

                if (SETTING_MUTE.get()) MutePartySpam.muteNextNMessages.incrementAndGet()

                val old = prevMelodyMessage.put(event.name, event.text) ?: return@on
                Scheduler.scheduleBeforePacket {
                    ChatUtils.deleteMessage(old)
                }
            } else lastMessages[event.name] = event.userMessage
        }
    }
}