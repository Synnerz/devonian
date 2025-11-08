package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.WorldFeature

object PrinceKilled : WorldFeature("princeKilled", "catacombs") {
    private val princeRegex = "^A Prince falls\\. \\+1 Bonus Score$".toRegex()
    private var messageSent = false

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(princeRegex) ?: return@on
            if (messageSent) return@on

            ChatUtils.command("pc Prince Killed!")
            messageSent = true
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        messageSent = false
    }
}