package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.mixin.accessor.ChatComponentAccessor
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals
import net.minecraft.client.GuiMessage
import net.minecraft.client.GuiMessageTag
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import java.util.IdentityHashMap
import kotlin.math.roundToInt

object ChatUtils {
    const val prefix = "&7[&cDevonian&7]"
    val chatLineIds = mutableMapOf<GuiMessage, Int>()
    val lineCache = IdentityHashMap<GuiMessage.Line, GuiMessage>()
    val chatComponentAccessor get() = Minecraft.getInstance().gui?.chat as ChatComponentAccessor
    val chatGui get() = Minecraft.getInstance().gui?.chat

    data class TextComponent(var text: Component, var id: Int = 0)

    fun literal(string: String): MutableComponent {
        return Component.literal(string.replace("&", "§"))
    }

    @JvmOverloads
    fun fromText(text: Component, id: Int = 0): TextComponent {
        return TextComponent(text, id)
    }

    fun sendMessageWithId(message: Component, id: Int) {
        val gui = chatGui ?: return

        gui.addMessage(message)

        chatLineIds[chatComponentAccessor.messages[0]] = id
    }

    fun sendMessage(message: Component) {
        Minecraft.getInstance().player?.displayClientMessage(message, false)
    }

    @JvmOverloads
    fun sendMessage(message: String, withPrefix: Boolean = false) {
        val toAdd = if (withPrefix) "$prefix " else ""

        sendMessage(literal("${toAdd}$message"))
    }

    fun removeLines(cb: (GuiMessage) -> Boolean) {
        var removedLine = false
        val messageList = chatComponentAccessor.messages?.listIterator() ?: return

        while (messageList.hasNext()) {
            val msg = messageList.next()
            if (!cb(msg)) continue

            messageList.remove()
            chatLineIds.remove(msg)
            removedLine = true
        }

        if (!removedLine) return

        chatComponentAccessor.invokeRefresh()
    }

    fun editLines(cb: (GuiMessage) -> Boolean, replaceWith: TextComponent) {
        var editedLine = false
        val indicator =
            if (Minecraft.getInstance().isSingleplayer) GuiMessageTag.systemSinglePlayer()
            else GuiMessageTag.system()
        val messageList = chatComponentAccessor.messages?.listIterator() ?: return

        while (messageList.hasNext()) {
            val msg = messageList.next()
            if (!cb(msg)) continue

            editedLine = true
            messageList.remove()
            chatLineIds.remove(msg)

            val line = GuiMessage(msg.addedTime, replaceWith.text, null, indicator)
            chatLineIds[line] = replaceWith.id
            messageList.add(line)
        }

        if (!editedLine) return

        chatComponentAccessor.invokeRefresh()
    }

    fun centerTextPadding(text: String): String {
        val textRenderer = Minecraft.getInstance().font
        val chatWidth = chatGui?.width ?: 0
        val textWidth = textRenderer.width(text)
        if (textWidth >= chatWidth) return text

        val padding = (chatWidth - textWidth) / 2f
        val paddingBuilder = StringBuilder().apply {
            repeat((padding / textRenderer.width(" ")).roundToInt()) {
                append(' ')
            }
        }

        return paddingBuilder.toString()
    }

    @JvmOverloads
    fun command(command: String, clientSide: Boolean = false) {
        if (!clientSide) return Minecraft.getInstance().connection!!.sendCommand(command)
        ClientCommandInternals.executeCommand(command)
    }

    fun say(message: String) = Minecraft.getInstance().connection?.sendChat(message)

    fun getMessageFromLine(line: GuiMessage.Line): GuiMessage? = lineCache[line]
}