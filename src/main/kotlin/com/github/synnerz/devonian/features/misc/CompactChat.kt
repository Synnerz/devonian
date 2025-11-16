package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils.clearCodes

// FIXME: whenever using `removeLines` it will always be worse in performance
//  due to it being O(n), find some way to make this functionality work without it being too annoying to code
//  in order for it to not be as performance intensive in the future.
object CompactChat : Feature(
    "compactChat",
    "Stacks the messages if they are repeated and adds the amount of times it was repeated"
) {
    private val chatList = mutableMapOf<String, Pair<Int, Long>>()

    override fun initialize() {
        on<ChatEvent> { event ->
            val msg = event.message
            if (msg.trim().isEmpty()) return@on

            val data = chatList[msg]
            val lastTime = data?.second

            if (lastTime != null && System.currentTimeMillis() - lastTime < 60_000) {
                val count = data.first + 1
                Scheduler.scheduleTask(0) {
                    ChatUtils.removeLines {
                        it.content?.string?.clearCodes() == msg || ChatUtils.chatLineIds[it] == msg.hashCode()
                    }
                    event.cancel()
                    ChatUtils.sendMessageWithId(
                        event.text.copy().append(ChatUtils.literal(" &7($count)")),
                        msg.hashCode()
                    )
                    chatList[msg] = Pair(count, System.currentTimeMillis())
                }
                return@on
            }

            chatList[msg] = Pair(1, System.currentTimeMillis())
        }
    }
}