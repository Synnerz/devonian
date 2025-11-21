package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.features.Feature
import net.minecraft.ChatFormatting
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

    data class MessageHistory(var times: Int = 0, var lastTime: Long = 0L)

    fun compactText(text: Component): Component {
        if (!isEnabled()) return text
        val string = text.string
        if (string.isBlank()) return text
        val cachedData = chatHistory.getOrPut(string) { MessageHistory(0, System.currentTimeMillis()) }
        if (System.currentTimeMillis() - cachedData.lastTime > 60_000) cachedData.times = 0

        cachedData.times++
        cachedData.lastTime = System.currentTimeMillis()
        if (cachedData.times <= 1) return text

        val textCopy = text.copy()
        val iter = ChatUtils.chatComponentAccessor.messages.listIterator()

        while (iter.hasNext()) {
            val line = iter.next()
            val contentCopy = line.content.copy()

            contentCopy.siblings.removeIf { it.contents is CompactChatComponent }

            if (contentCopy.string.equals(textCopy.string)) {
                iter.remove()
                ChatUtils.chatComponentAccessor.invokeRefresh()
                break
            }
        }

        return textCopy.append(CompactChatComponent.of(cachedData.times).withStyle(STYLE))
    }

    fun clearHistory() {
        chatHistory.clear()
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