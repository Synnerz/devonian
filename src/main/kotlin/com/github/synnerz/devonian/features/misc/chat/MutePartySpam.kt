package com.github.synnerz.devonian.features.misc.chat

import com.github.synnerz.devonian.api.events.ChatChannelEvent
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.OpenEditor
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import java.util.concurrent.CopyOnWriteArrayList

object MutePartySpam : Feature(
    "mutePartySpam",
    "Mutes the pings from useless party messages.",
    subcategory = "Chat",
) {
    private var isEditing = false

    private val SETTING_EDIT = addButton(
        {
            if (isEditing) return@addButton
            isEditing = true
            OpenEditor.edit(messages) {
                messages.clear()
                messages.addAll(it)
                isEditing = false
            }
        },
        description = "Will open up a new window.",
        displayName = "Edit Mute List",
    )

    private var CONFIG_KEY = "mutePartySpam"

    private val messages = CopyOnWriteArrayList<String>()

    var muteNextMessage = false

    override fun initialize() {
        Config.set(CONFIG_KEY, JsonArray().also { arr ->
            listOf(
                "Bonzo Procced",
                "Phoenix Procced",
                "Spirit Procced",
                "Leaped to ",
                "Leaping to ",
                "I\\'m leaping to ",
                "[Leaped]: ➜",
                "Gained ",
                "At ",
                "at ",
                "Entering ",
                "BERS TEAM --> ",
                "Power: ",
                "UwUaddons",
                "Used ",
                "No Shop!",
                "No Triangle!",
                "No Equals!",
                "No Slash!",
                "No X Cannon!",
                "No X!",
                "FRESH",
                "[IQ]",
                "No xCannon!",
                "X Cannon",
                "[Skyblocker]",
                "Broken",
                "/tp ",
                "Zen",
                "MeowAddons",
                "!",
                "[!]",
            ).forEach(arr::add)
        })

        Config.onAfterLoad {
            Config.get<List<JsonPrimitive>>(CONFIG_KEY)?.mapNotNull {
                if (it.isString) it.asString else null
            }?.let {
                messages.addAll(it)
            }
        }

        Config.onPreSave {
            val arr = JsonArray()

            messages.forEach {
                arr.add(it)
            }

            Config.set(CONFIG_KEY, arr)
        }

        on<ChatChannelEvent.PartyChatEvent> { event ->
            if (messages.any { event.userMessage.startsWith(it) }) muteNextMessage = true
        }

        on<SoundPlayEvent> { event ->
            if (muteNextMessage) {
                if (
                    event.underlyingEvent == SoundEvents.EXPERIENCE_ORB_PICKUP &&
                    event.category == SoundSource.PLAYERS &&
                    event.volume == 1f &&
                    event.pitch == 1f
                ) event.cancel()
            }
            muteNextMessage = false
        }
    }
}