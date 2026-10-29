package com.github.synnerz.devonian.features.misc.chat

import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.multiplayer.chat.GuiMessageTag

object HideChatTags : Feature(
    "hideChatTags",
    "Hides Minecraft's chat tags when hovering over certain messages. e.g. 'Server message.'",
    Categories.MISC,
    subcategory = "Chat",
) {
    private val SETTING_MODE = addSelection(
        "mode",
        0,
        listOf("Vanilla", "All"),
        "Which tags to hide.",
        "Hidden Tags",
    )

    fun shouldHide(tag: GuiMessageTag): Boolean {
        if (!isEnabled()) return false
        when (SETTING_MODE.get()) {
            0 -> {
                if (tag === GuiMessageTag.system()) return true
                if (tag === GuiMessageTag.systemSinglePlayer()) return true
                if (tag === GuiMessageTag.chatNotSecure()) return true
                if (tag.logTag == "Modified") return true
                if (tag === GuiMessageTag.chatError()) return true
                return false
            }

            else -> return true
        }
    }
}