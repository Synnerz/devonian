package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.features.Feature
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
    "Stacks the messages if they are repeated and adds the amount of times it was repeated"
) {
    private val STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY)
    private val chatHistory = hashMapOf<String, MessageHistory>()
    private val recentMessages = hashMapOf<String, Int>()
    private val textContentCache = IdentityHashMap<GuiMessage, String?>()

    data class MessageHistory(var count: Int = 0, var lastTime: Long = 0L)

    fun compactText(text: Component): Component {
        if (!isEnabled()) return text
        val string = text.string
        if (string.isBlank()) return text
        val time = System.currentTimeMillis()
        val cachedData = chatHistory.getOrPut(string) { MessageHistory(0, time) }
        if (time - cachedData.lastTime > 60_000) cachedData.count = 0

        cachedData.count++
        cachedData.lastTime = time
        val textCopy = text.copy()
        val incomingMsg = textCopy.string
        if (cachedData.count <= 1) {
            recentMessages[incomingMsg] = 1
            return text
        }

        val iter = ChatUtils.chatComponentAccessor.messages.listIterator()
        var refresh = false

        while (iter.hasNext()) {
            val line = iter.next()
            val msg = textContentCache.getOrPut(line) {
                val contentCopy = line.content.copy()
                contentCopy.siblings.removeIf { it.contents is CompactChatComponent }

                return@getOrPut contentCopy.string
            } ?: continue

            if (msg == incomingMsg) {
                val count = recentMessages.merge(msg, 1, Int::plus) ?: 1
                if (count == 1) {
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
        if (refresh) ChatUtils.chatComponentAccessor.invokeRefresh()
        else recentMessages[incomingMsg] = 1

        return textCopy.append(CompactChatComponent.of(cachedData.count).withStyle(STYLE))
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
class CompactChatComponent(val times: Int = 0) : PlainTextContents {
    companion object {
        @JvmStatic
        fun of(times: Int): MutableComponent = MutableComponent.create(CompactChatComponent(times))
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