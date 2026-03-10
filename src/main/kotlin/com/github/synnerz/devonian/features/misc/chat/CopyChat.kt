package com.github.synnerz.devonian.features.misc.chat

import com.github.synnerz.devonian.ChatComponentAccessor2
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import net.minecraft.client.gui.screens.ChatScreen

object CopyChat : Feature(
    "copyChat",
    "Right click to copy a message in chat.",
    Categories.VANILLA_TWEAKS,
    subcategory = "Chat",
) {
    override fun initialize() {
        on<GuiClickEvent> { event ->
            if (!event.state || event.mbtn != 1) return@on

            val screen = event.screen
            if (screen !is ChatScreen) return@on

            val msg = (minecraft.gui.chat as? ChatComponentAccessor2)?.`devonian$getLastHoveredMessage`() ?: return@on
            val text = msg.content
            val str = text.string.clearCodes()

            minecraft.keyboardHandler.clipboard = str
            Scheduler.scheduleTask(2) {
                ChatUtils.sendMessage("&aCopied message to clipboard", true)
            }
        }
    }
}