package com.github.synnerz.devonian.features.misc.chat

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.gui.screens.ChatScreen

object CopyChat : Feature(
    "copyChat",
    "Right click to copy a message in chat.",
    subcategory = "Chat",
) {
    override fun initialize() {
        on<GuiClickEvent> { event ->
            if (!event.state || event.mbtn != 1) return@on

            val screen = event.screen
            if (screen !is ChatScreen) return@on

            ChatUtils.sendMessage("&4Broken in 1.21.11 for now :(")

            // val finder = ActiveTextCollector.ClickableStyleFinder(
            //     minecraft.font,
            //     event.mx.toInt(),
            //     event.my.toInt(),
            // ).includeInsertions(true)
            // minecraft.gui.chat.captureClickableText(
            //     finder,
            //     minecraft.window.guiScaledHeight,
            //     minecraft.gui.guiTicks,
            //     true,
            // )

            // val text: Component = TODO()
            // val comp = ChatUtils.getMessageFromLine(text) ?: return@on
            // val str = comp.content.string.clearCodes()
            //
            // minecraft.keyboardHandler.clipboard = str
            // ChatUtils.sendMessage("&aCopied message to clipboard", true)
        }
    }
}