package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.KeyReleaseEvent
import com.github.synnerz.devonian.api.events.MouseReleaseEvent
import com.github.synnerz.devonian.api.events.MouseScrollEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object PeekChatKeybind : Feature(
    "peekChatKeybind",
    "Allows you to quickly peek into the chat screen without opening the textinput (change the keybind in minecraft controls)",
    Categories.VANILLA_TWEAKS,
) {
    val keybind = KeyMappingHelper.registerKeyMapping(
        KeyMapping(
            "key.devonian.peekchatkey",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
    )

    override fun initialize() {
        on<MouseScrollEvent> { event ->
            if (!keybind.isDown) return@on

            // val d = event.delta * (if (minecraft.hasShiftDown()) 1.0 else 7.0)
            val d = event.delta
            minecraft.gui.hud.chat.scrollChat(d.toInt())

            event.cancel()
        }

        on<KeyReleaseEvent> { event ->
            if (!keybind.matches(event.underlying)) return@on

            minecraft.gui.hud.chat.resetChatScroll()
        }

        on<MouseReleaseEvent> { event ->
            if (!keybind.matchesMouse(event.mcEvent)) return@on

            minecraft.gui.hud.chat.resetChatScroll()
        }
    }
}