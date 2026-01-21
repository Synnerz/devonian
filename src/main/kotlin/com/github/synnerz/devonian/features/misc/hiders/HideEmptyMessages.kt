package com.github.synnerz.devonian.features.misc.hiders

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.features.Feature

object HideEmptyMessages : Feature(
    "hideEmptyMessages",
    subcategory = "Hiders",
) {
    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.message.isBlank()) event.cancel()
        }
    }
}