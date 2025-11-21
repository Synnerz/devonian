package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.ChatComponentAccessor
import net.minecraft.client.gui.screens.ChatScreen

object CopyChat : Feature(
    "copyChat",
    "Right click to copy a message in chat."
) {
    override fun initialize() {
        on<GuiClickEvent> { event ->
            if (!event.state || event.mbtn != 1) return@on

            val screen = event.screen
            if (screen !is ChatScreen) return@on
            val chatHud = Devonian.minecraft.gui.chat as ChatComponentAccessor
            val dx = chatHud.toChatLineMX(event.mx)
            val dy = chatHud.toChatLineMY(event.my)
            val idx = chatHud.getMessageLineIdx(dx, dy)
            if (idx < 0 || idx >= chatHud.visibleMessages.size) return@on

            val text = chatHud.visibleMessages.getOrNull(idx) ?: return@on
            val str = StringBuilder()
            text.content.accept { _, _, code ->
                str.append(Character.toChars(code))
                true
            }
            for (jdx in 1..9) {
                val lineIdx = idx - jdx
                val msg = chatHud.visibleMessages.getOrNull(lineIdx) ?: continue
                val strBuilt = buildString {
                    msg.content.accept { _, _, code ->
                        append(Character.toChars(code))
                        true
                    }
                }
                if (!strBuilt.startsWith(" ")) break
                str.append(strBuilt)
            }

            minecraft.keyboardHandler.clipboard = "$str"
            ChatUtils.sendMessage("&aCopied message to clipboard", true)
        }
    }
}