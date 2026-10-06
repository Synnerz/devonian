package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.ChatComponentAccessor2
import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.features.misc.chat.CompactChatComponent
import com.github.synnerz.devonian.mixin.accessor.ChatComponentAccessor
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.client.multiplayer.chat.GuiMessageSource
import net.minecraft.client.multiplayer.chat.GuiMessageTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import java.util.*
import kotlin.math.roundToInt

object ChatUtils {
    const val prefix = "&8&l[&3&lDevonian&8&l]&r"
    val chatLineIds = mutableMapOf<GuiMessage, Int>()
    val lineCache = IdentityHashMap<GuiMessage.Line, GuiMessage>()
    val removedLines: MutableSet<GuiMessage> = Collections.newSetFromMap(IdentityHashMap())
    val replacedLines = IdentityHashMap<GuiMessage, GuiMessage>()
    val chatComponentAccessor get() = Minecraft.getInstance().gui.hud.chat as ChatComponentAccessor
    val chatComponentAccessor2 get() = Minecraft.getInstance().gui.hud.chat as ChatComponentAccessor2
    val chatGui get() = Minecraft.getInstance().gui.hud.chat

    data class TextComponent(var text: Component, var id: Int = 0)

    fun literal(string: String): MutableComponent {
        return Component.literal(string.replace("&", "§"))
    }

    @JvmOverloads
    fun fromText(text: Component, id: Int = 0): TextComponent {
        return TextComponent(text, id)
    }

    fun sendMessageWithId(message: Component, id: Int) {
        if (!Devonian.minecraft.isMultiplayerServer)
            chatGui.addClientSystemMessage(message)
        else
            chatGui.addServerSystemMessage(message)

        chatLineIds[chatComponentAccessor.dv_getAllMessages()[0]] = id
    }

    fun sendMessage(message: Component) {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().player?.sendSystemMessage(message)
        }
    }

    @JvmOverloads
    fun sendMessage(message: String, withPrefix: Boolean = false) {
        val toAdd = if (withPrefix) "$prefix " else ""

        sendMessage(literal("${toAdd}$message"))
    }

    fun sendActionbar(message: Component) {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().player?.sendOverlayMessage(message)
        }
    }

    fun sendActionbar(message: String) = sendActionbar(literal(message))

    fun removeLines(cb: (GuiMessage) -> Boolean) {
        var removedLine = false
        val messageList = chatComponentAccessor.dv_getAllMessages()?.listIterator() ?: return
        var jdx = 0

        while (messageList.hasNext()) {
            val msg = messageList.next()
            if (jdx >= 500) break
            jdx++
            if (!cb(msg)) continue

            messageList.remove()
            chatLineIds.remove(msg)
            removedLines.add(msg)
            removedLine = true
        }

        if (!removedLine) return

        refreshChat()
    }

    fun editLines(cb: (GuiMessage) -> Boolean, replaceWith: TextComponent) {
        var editedLine = false
        val indicator =
            if (!Minecraft.getInstance().isMultiplayerServer) GuiMessageTag.systemSinglePlayer()
            else GuiMessageTag.system()
        val messageList = chatComponentAccessor.dv_getAllMessages()?.listIterator() ?: return
        var jdx = 0

        while (messageList.hasNext()) {
            val msg = messageList.next()
            if (jdx >= 500) break
            jdx++
            if (!cb(msg)) continue

            editedLine = true
            messageList.remove()
            chatLineIds.remove(msg)

            val line = GuiMessage(msg.addedTime, replaceWith.text, null, GuiMessageSource.SYSTEM_SERVER, indicator)
            chatLineIds[line] = replaceWith.id
            removedLines.add(msg)
            replacedLines[msg] = line
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
        ClientCommandInternals.executeCommand(
            command,
            Devonian.minecraft.connection!!.suggestionsProvider as FabricClientCommandSource,
            null
        )
    }

    fun say(message: String) {
        val connection = Minecraft.getInstance().connection ?: return
        if (message.startsWith("/")) return connection.sendCommand(message.drop(1))

        connection.sendChat(message)
    }

    fun getMessageFromLine(line: GuiMessage.Line): GuiMessage? = lineCache[line]

    fun deleteMessage(comp: Component, max: Int = 20) {
        val iter = chatComponentAccessor.dv_getAllMessages().listIterator()
        var i = max

        while (--i >= 0 && iter.hasNext()) {
            val line = iter.next()
            if (
                line.content === comp ||
                (line.content.siblings.lastOrNull()?.contents as? CompactChatComponent)?.orig === comp
            ) {
                iter.remove()
                removedLines.add(line)
                refreshChat()
                break
            }
        }
    }

    private var needRefresh = 0

    private fun doRefresh() {
        val msgs = chatComponentAccessor.dv_getTrimmedMessages().listIterator()

        var foundHead = true
        var shouldRemove = false
        var removeC = removedLines.size
        var replaceC = replacedLines.size
        while (msgs.hasNext() && (removeC > 0 || replaceC > 0)) {
            val line = msgs.next()
            if (!foundHead) {
                if (!line.endOfEntry) {
                    if (shouldRemove) msgs.remove()
                    continue
                } else {
                    foundHead = true
                    shouldRemove = false
                }
            }

            val msg = lineCache[line] ?: continue
            foundHead = false

            if (msg in removedLines) {
                removeC--
                msgs.remove()
                shouldRemove = true
            }

            val replaced = replacedLines[msg] ?: continue
            replaceC--
            chatComponentAccessor2.`devonian$injectAddMessage`(replaced,  msgs)
        }

        removedLines.clear()
        replacedLines.clear()
    }

    fun refreshChat() {
        needRefresh++
        if (needRefresh == 1) doRefresh()
    }

    fun initialize() {
        EventBus.on<TickEvent> {
            if (needRefresh > 1) doRefresh()
            needRefresh = 0
        }
    }
}