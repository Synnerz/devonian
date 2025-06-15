package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.utils.ChatUtils
import com.github.synnerz.devonian.utils.JsonUtils
import net.minecraft.text.ClickEvent
import net.minecraft.text.Style
import net.minecraft.text.Text

// TODO: make it able to unregister events and re-register
//  as well as making location based registry

open class Feature(val configName: String) {
    val minecraft = Devonian.minecraft
    val id = 256652 + Devonian.features.size
    private val style = Style.EMPTY.withClickEvent(ClickEvent.RunCommand("devonian config $id"))
    private var displayed = false

    init {
        Devonian.features.add(this)
        JsonUtils.set(configName, false)
    }

    open fun initialize() {}

    fun isEnabled(): Boolean {
        return JsonUtils.get(configName) ?: false
    }

    fun setEnabled() {
        JsonUtils.set(configName, true)
        onToggle(true)
    }

    fun setDisabled() {
        JsonUtils.set(configName, false)
        onToggle(false)
    }

    fun toggle() {
        if (!isEnabled()) return setEnabled()
        setDisabled()
    }

    fun displayChat() {
        if (displayed) deleteChat()
        ChatUtils.sendMessageWithId(
            ChatUtils.literal("&7- &b$configName ${if (isEnabled()) "&a[\uD83D\uDDF8]" else "&c[x]"}").setStyle(style),
            id
        )
        displayed = true
    }

    fun onCommand(id: Int) {
        if (id != this.id) return

        toggle()
        ChatUtils.editLines(
            { ChatUtils.chatLineIds[it] == id },
            ChatUtils.fromText(
                ChatUtils.literal("&7- &b$configName ${if (isEnabled()) "&a[\uD83D\uDDF8]" else "&c[x]"}")
                    .setStyle(style),
                id)
        )
    }

    fun deleteChat() {
        ChatUtils.removeLines { ChatUtils.chatLineIds[it] == id }
        displayed = false
    }

    open fun onToggle(state: Boolean) {}

    fun sendMessage(msg: String) {
        minecraft.player?.sendMessage(Text.literal(msg), false)
    }
}