package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.features.misc.chat.CompactChatComponent
import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.mixin.accessor.ChatComponentAccessor
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals
import net.minecraft.client.GuiMessage
import net.minecraft.client.GuiMessageTag
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import java.util.*
import kotlin.math.roundToInt

object ChatUtils {
    const val prefix = "&8[&3&lDevonian&8]"
    val chatLineIds = mutableMapOf<GuiMessage, Int>()
    val lineCache = IdentityHashMap<GuiMessage.Line, GuiMessage>()
    val chatComponentAccessor get() = Minecraft.getInstance().gui.chat as ChatComponentAccessor
    val chatGui get() = Minecraft.getInstance().gui.chat

    data class TextComponent(var text: Component, var id: Int = 0)

    fun literal(string: String): MutableComponent {
        return Component.literal(string.replace("&", "§"))
    }

    @JvmOverloads
    fun fromText(text: Component, id: Int = 0): TextComponent {
        return TextComponent(text, id)
    }

    fun sendMessageWithId(message: Component, id: Int) {
        chatGui.addMessage(message)

        chatLineIds[chatComponentAccessor.messages[0]] = id
    }

    fun sendMessage(message: Component) {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().player?.displayClientMessage(message, false)
        }
    }

    @JvmOverloads
    fun sendMessage(message: String, withPrefix: Boolean = false) {
        val toAdd = if (withPrefix) "$prefix " else ""

        sendMessage(literal("${toAdd}$message"))
    }

    fun sendActionbar(message: Component) {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().player?.displayClientMessage(message, true)
        }
    }

    fun sendActionbar(message: String) = sendActionbar(literal(message))

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

        refreshChat()
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

        refreshChat()
    }

    fun centerTextPadding(text: String): String {
        val textRenderer = Minecraft.getInstance().font
        val ww = Devonian.minecraft.options.chatWidth()
        val chatWidth = ChatComponent.getWidth(ww.get())
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

    fun say(message: String) {
        val connection = Minecraft.getInstance().connection ?: return
        if (message.startsWith("/")) return connection.sendCommand(message.drop(1))

        connection.sendChat(message)
    }

    fun getMessageFromLine(line: GuiMessage.Line): GuiMessage? = lineCache[line]

    fun deleteMessage(comp: Component, max: Int = 20) {
        val iter = chatComponentAccessor.messages.listIterator()
        var i = max

        while (--i >= 0 && iter.hasNext()) {
            val line = iter.next()
            if (
                line.content === comp ||
                (line.content.siblings.lastOrNull()?.contents as? CompactChatComponent)?.orig === comp
            ) {
                iter.remove()
                refreshChat()
                break
            }
        }
    }

    private var needRefresh = 0

    fun refreshChat() {
        needRefresh++
        if (needRefresh == 1) chatComponentAccessor.invokeRefresh()
    }

    fun initialize() {
        EventBus.on<TickEvent> {
            if (needRefresh > 1) chatComponentAccessor.invokeRefresh()
            needRefresh = 0
        }
    }
}