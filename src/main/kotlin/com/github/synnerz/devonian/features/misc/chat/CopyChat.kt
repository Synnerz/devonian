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
    private val SETTING_TRIM = addSwitch(
        "trimCopiedMessage",
        true,
        "Removes spaces from start/end of copied messages.",
        "Trim Copied Messages",
    )

    override fun initialize() {
        on<GuiClickEvent> { event ->
            if (!event.state || event.mbtn != 1) return@on

            val screen = event.screen
            if (screen !is ChatScreen) return@on

            val msg = (minecraft.gui.hud.chat as? ChatComponentAccessor2)?.`devonian$getLastHoveredMessage`() ?: return@on
            val text = msg.content
            var str = text.string.clearCodes()
            if (SETTING_TRIM.get()) str = str.trim()

            minecraft.keyboardHandler.clipboard = str
            Scheduler.scheduleTask(2) {
                ChatUtils.sendMessage("&aCopied message to clipboard", true)
            }
        }
    }
}