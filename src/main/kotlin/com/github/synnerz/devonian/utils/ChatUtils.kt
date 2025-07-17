package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.mixin.accessor.ChatHudAccessor
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.client.gui.hud.MessageIndicator
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import kotlin.math.roundToInt

object ChatUtils {
    const val prefix = "&7[&cDevonian&7]"
    val chatLineIds = mutableMapOf<ChatHudLine, Int>()
    val chatHudAccessor get() = MinecraftClient.getInstance().inGameHud?.chatHud as ChatHudAccessor
    val chatGui get() = MinecraftClient.getInstance().inGameHud?.chatHud

    data class TextComponent(var text: Text, var id: Int = 0)

    fun literal(string: String): MutableText {
        return Text.literal(string.replace("&", "§"))
    }

    @JvmOverloads
    fun fromText(text: Text, id: Int = 0): TextComponent {
        return TextComponent(text, id)
    }

    fun sendMessageWithId(message: Text, id: Int) {
        val gui = chatGui ?: return

        gui.addMessage(message)

        chatLineIds[chatHudAccessor.messages[0]] = id
    }

    fun sendMessage(message: Text) {
        MinecraftClient.getInstance().player?.sendMessage(message, false)
    }

    @JvmOverloads
    fun sendMessage(message: String, withPrefix: Boolean = false) {
        val toAdd = if (withPrefix) "$prefix " else ""

        sendMessage(literal("${toAdd}$message"))
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

        try {
            chatHudAccessor.invokeRefresh()
        } catch (e: ConcurrentModificationException) {
            e.printStackTrace()
        }
    }

    fun editLines(cb: (ChatHudLine) -> Boolean, replaceWith: TextComponent) {
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

            val line = ChatHudLine(msg.creationTick, replaceWith.text, null, indicator)
            chatLineIds[line] = replaceWith.id
            messageList.add(line)
        }

        if (!editedLine) return

        chatHudAccessor.invokeRefresh()
    }

    fun centerTextPadding(text: String): String {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val chatWidth = chatGui?.width ?: 0
        val textWidth = textRenderer.getWidth(text)
        if (textWidth >= chatWidth) return text

        val padding = (chatWidth - textWidth) / 2f
        val paddingBuilder = StringBuilder().apply {
            repeat((padding / textRenderer.getWidth(" ")).roundToInt()) {
                append(' ')
            }
        }

        return paddingBuilder.toString()
    }

    @JvmOverloads
    fun command(command: String, clientSide: Boolean = false) {
        if (!clientSide) return MinecraftClient.getInstance().networkHandler!!.sendChatCommand(command)
        ClientCommandInternals.executeCommand(command)
    }

    fun say(message: String) = MinecraftClient.getInstance().networkHandler?.sendChatMessage(message)
}