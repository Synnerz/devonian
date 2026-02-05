package com.github.synnerz.devonian.features.misc.chat

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import net.minecraft.ChatFormatting
import net.minecraft.client.GuiMessage
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents
import java.util.*

// Credits to <https://github.com/caoimhebyrne/compact-chat>
// Licensed under the MIT license
object CompactChat : Feature(
    "compactChat",
    "Stacks the messages if they are repeated and adds the amount of times it was repeated.",
    subcategory = "Chat",
) {
    private val STYLE = Style.EMPTY
        .withColor(ChatFormatting.GRAY)
        .withBold(false)
        .withItalic(false)
        .withObfuscated(false)
        .withStrikethrough(false)
        .withUnderlined(false)
    private val chatHistory = hashMapOf<String, MessageHistory>()
    private val recentMessages = hashMapOf<String, Int>()
    private val textContentCache = IdentityHashMap<GuiMessage, String?>()
    private val nonLineBreakMessage = "\\w".toRegex()

    data class MessageHistory(var count: Int = 0, var lastTime: Long = 0L, var lastCheck: GuiMessage? = null)

    fun compactText(text: Component): Component {
        if (!isEnabled()) return text

        val textStrRaw = text.string
        val textStr = textStrRaw.clearCodes()
        if (textStr.isBlank()) return text

        val time = System.currentTimeMillis()
        val cachedData = chatHistory.getOrPut(textStr) { MessageHistory(0, time) }
        if (time - cachedData.lastTime > 60_000) cachedData.count = 0

        cachedData.count++
        cachedData.lastTime = time
        if (cachedData.count <= 1) {
            recentMessages[textStr] = 1
            cachedData.lastCheck = ChatUtils.chatComponentAccessor.messages.firstOrNull()
            return text
        }

        val iter = ChatUtils.chatComponentAccessor.messages.listIterator()
        var refresh = false
        var first: GuiMessage? = null

        while (iter.hasNext()) {
            val line = iter.next()
            if (first == null) first = line

            if (line === cachedData.lastCheck) break

            val msg = textContentCache.getOrPut(line) {
                val contentCopy = line.content.copy()
                contentCopy.siblings.removeIf { it.contents is CompactChatComponent }

                return@getOrPut contentCopy.string.clearCodes()
            } ?: continue

            if (msg == textStr) {
                val count = recentMessages.merge(msg, 1, Int::plus) ?: 1
                if (count == 1 || nonLineBreakMessage.containsMatchIn(msg)) {
                    iter.remove()
                } else {
                    // Immutable java.lang.UnsupportedOperationException
                    // line.content.siblings.removeIf { it.contents is CompactChatComponent }
                    iter.set(
                        GuiMessage(
                            line.addedTime,
                            line.content.copy()
                                .also { it.siblings.removeIf { it.contents is CompactChatComponent } },
                            line.signature,
                            line.tag
                        )
                    )
                }
                refresh = true
                break
            }
        }
        if (refresh) ChatUtils.refreshChat()
        else recentMessages[textStr] = 1

        cachedData.lastCheck = first

        return text.copy().append(CompactChatComponent.of(text, cachedData.count).withStyle(STYLE))
    }

    fun clearHistory() {
        chatHistory.clear()
        textContentCache.clear()
    }

    override fun initialize() {
        on<ClientThreadServerTickEvent> {
            recentMessages.clear()
        }
    }
}

// Credits to <https://github.com/caoimhebyrne/compact-chat>
// Licensed under the MIT license
class CompactChatComponent(val orig: Component, val times: Int = 0) : PlainTextContents {
    companion object {
        @JvmStatic
        fun of(orig: Component, times: Int): MutableComponent =
            MutableComponent.create(CompactChatComponent(orig, times))
    }

    override fun text(): String = " ($times)"

    override fun <T : Any?> visit(
        styledContentConsumer: FormattedText.StyledContentConsumer<T>,
        style: Style
    ): Optional<T> {
        return styledContentConsumer.accept(style, text())
    }

    override fun <T : Any?> visit(contentConsumer: FormattedText.ContentConsumer<T>): Optional<T> {
        return contentConsumer.accept(text())
    }

    override fun toString(): String = "CompactChatComponent(x$times)"
}