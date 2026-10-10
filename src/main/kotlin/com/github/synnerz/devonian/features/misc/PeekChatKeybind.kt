package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.api.events.GuiKeyUpEvent
import com.github.synnerz.devonian.api.events.GuiScrollEvent
import com.github.synnerz.devonian.api.events.KeyReleaseEvent
import com.github.synnerz.devonian.api.events.MouseReleaseEvent
import com.github.synnerz.devonian.api.events.MouseScrollEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.sdl.SDLKeycode

object PeekChatKeybind : Feature(
    "peekChatKeybind",
    "Allows you to quickly peek into the chat screen without opening the textinput (change the keybind in minecraft controls)",
    Categories.VANILLA_TWEAKS,
) {
    private val SETTING_OTHER_GUIS = addSwitch(
        "otherGuis",
        false,
        "Whether the peek chat keybind should work in other guis.",
        "Work in GUIs",
    )

    private val keybind = KeyMappingHelper.registerKeyMapping(
        KeyMapping(
            "key.devonian.peekchatkey",
            SDLKeycode.SDLK_UNKNOWN,
            Devonian.keybindCategory
        )
    )

    private var peekingGui = false
    fun isPeeking() = isEnabled() && (keybind.isDown || peekingGui)

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

        on<GuiKeyDownEvent> { event ->
            if (!keybind.matches(event.event)) return@on

            peekingGui = true
        }.setEnabled(SETTING_OTHER_GUIS.state)

        on<GuiKeyUpEvent> { event ->
            if (!keybind.matches(event.event)) return@on

            peekingGui = false
            minecraft.gui.hud.chat.resetChatScroll()
        }.setEnabled(SETTING_OTHER_GUIS.state)

        on<GuiClickEvent> { event ->
            if (!keybind.matchesMouse(event.event)) return@on

            peekingGui = event.state
            if (!peekingGui) minecraft.gui.hud.chat.resetChatScroll()
        }.setEnabled(SETTING_OTHER_GUIS.state)

        on<ClientContainerCloseEvent> {
            if (peekingGui) minecraft.gui.hud.chat.resetChatScroll()
            peekingGui = false
        }.setEnabled(SETTING_OTHER_GUIS.state)

        on<GuiScrollEvent> { event ->
            if (!peekingGui) return@on

            minecraft.gui.hud.chat.scrollChat(event.delta.toInt())

            event.cancel()
        }.setEnabled(SETTING_OTHER_GUIS.state)
    }
}