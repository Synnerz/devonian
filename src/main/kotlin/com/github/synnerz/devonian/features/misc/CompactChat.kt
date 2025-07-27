package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.events.ChatEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.ChatUtils
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import kotlin.concurrent.thread

object CompactChat : Feature("compactChat") {
    private val chatList = mutableMapOf<String, Pair<Int, Long>>()

    override fun initialize() {
        on<ChatEvent> { event ->
            val msg = event.message
            if (msg.trim().isEmpty()) return@on

            val data = chatList[msg]
            val lastTime = data?.second

            if (lastTime != null && System.currentTimeMillis() - lastTime < 60_000) {
                val count = data.first + 1
                thread {
                    ChatUtils.removeLines {
                        it.content?.string?.clearCodes() == msg || ChatUtils.chatLineIds[it] == msg.hashCode()
                    }
                }
                event.cancel()
                ChatUtils.sendMessageWithId(
                    event.text.copy().append(ChatUtils.literal(" &7($count)")),
                    msg.hashCode()
                )
                chatList[msg] = Pair(count, System.currentTimeMillis())
                return@on
            }

            chatList[msg] = Pair(1, System.currentTimeMillis())
        }
    }
}