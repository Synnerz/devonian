package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.mixin.ChatHudAccessor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.client.gui.hud.MessageIndicator
import net.minecraft.text.Text

object ChatUtils {
    val chatLineIds = mutableMapOf<ChatHudLine, Int>()
    val chatHudAccessor get() = MinecraftClient.getInstance().inGameHud?.chatHud as ChatHudAccessor
    val chatGui get() = MinecraftClient.getInstance().inGameHud?.chatHud

    fun sendMessageWithId(message: Text, id: Int) {
        val gui = chatGui ?: return

        gui.addMessage(message)

        chatLineIds[chatHudAccessor.messages[0]] = id
    }

    fun removeLines(cb: (ChatHudLine) -> Boolean) {
        var removedLine = false
        val messageList = chatHudAccessor.messages?.listIterator() ?: return

        while (messageList.hasNext()) {
            val msg = messageList.next()
            if (!cb(msg)) continue

            messageList.remove()
            chatLineIds.remove(msg)
            removedLine = true
        }

        if (!removedLine) return

        chatHudAccessor.invokeRefresh()
    }

    fun editLines(cb: (ChatHudLine) -> Boolean, replaceWith: Text) {
        var editedLine = false
        val indicator =
            if (MinecraftClient.getInstance().isConnectedToLocalServer) MessageIndicator.singlePlayer()
            else MessageIndicator.system()
        val messageList = chatHudAccessor.messages?.listIterator() ?: return

        while (messageList.hasNext()) {
            val msg = messageList.next()
            if (!cb(msg)) continue

            editedLine = true
            messageList.remove()
            chatLineIds.remove(msg)

            val line = ChatHudLine(msg.creationTick, replaceWith, null, indicator)
            chatLineIds[line] = 100 // TODO: make Text wrapper for these
            messageList.add(line)
        }

        if (!editedLine) return

        chatHudAccessor.invokeRefresh()
    }
}