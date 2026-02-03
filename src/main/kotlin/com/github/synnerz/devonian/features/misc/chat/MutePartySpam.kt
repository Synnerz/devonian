package com.github.synnerz.devonian.features.misc.chat

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatChannelEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.OpenEditor
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

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

    val muteNextNMessages = atomic(0)

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
            if (messages.any { event.userMessage.startsWith(it) }) muteNextNMessages.incrementAndGet()
        }

        EventBus.on<SoundPlayEvent> { event ->
            if (
                event.underlyingEvent != SoundEvents.EXPERIENCE_ORB_PICKUP ||
                event.category != SoundSource.PLAYERS ||
                event.volume != 1f ||
                event.pitch != 1f
            ) return@on

            event.cancel()
            Scheduler.scheduleBeforePacket {
                var canceled = false
                muteNextNMessages.update {
                    if (it > 0) canceled = true
                    max(it - 1, 0)
                }
                if (canceled) return@scheduleBeforePacket

                minecraft.level?.playSeededSound(
                    minecraft.player,
                    event.x, event.y, event.z,
                    event.underlyingEvent,
                    event.category,
                    event.volume, event.pitch,
                    event.seed,
                )
            }
        }
    }
}