package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.ChatHudAccessor
import net.minecraft.client.gui.screen.ChatScreen

object CopyChat : Feature(
    "copyChat",
    "Right click to copy a message in chat."
) {
    override fun initialize() {

        on<GuiClickEvent> { event ->
            if (!event.state || event.mbtn != 1) return@on

            val screen = event.screen
            if (screen !is ChatScreen) return@on
            val chatHud = Devonian.minecraft.inGameHud.chatHud as ChatHudAccessor
            val dx = chatHud.toChatLineMX(event.mx)
            val dy = chatHud.toChatLineMY(event.my)
            val idx = chatHud.getMessageLineIdx(dx, dy).coerceIn(0..<chatHud.visibleMessages.size)

            val text = chatHud.visibleMessages[idx] ?: return@on
            val str = StringBuilder()
            text.content.accept { _, _, codept ->
                str.append(Character.toChars(codept))
                true
            }

            minecraft.keyboard.clipboard = "$str"
            ChatUtils.sendMessage("&aCopied message to clipboard", true)
        }
    }
}