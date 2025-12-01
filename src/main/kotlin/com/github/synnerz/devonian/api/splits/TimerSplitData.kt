package com.github.synnerz.devonian.api.splits

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.utils.StringUtils

data class TimerSplitData(
    val title: String? = null,
    val criteria: List<Regex>,
    val sendChat: Boolean = true,
    var boundTo: TimerSplitData? = null,
    var time: Long = 0L,
    val children: MutableList<TimerSplitData> = mutableListOf(),
    val chat: String? = title
) {
    constructor(title: String?, criteria: Regex, sendChat: Boolean = true, chat: String? = title)
            : this(title, listOf(criteria), sendChat, chat = chat)

    fun title(_format: Boolean = false): String? {
        if (children.isEmpty()) return title

        var str = title

        for (child in children)
            if (child.title != null) {
                if (_format) {
                    str += child.title.replace("$1", StringUtils.formatSeconds(child.seconds()))
                    continue
                }
                str += child.title.replace("$1", "${child.seconds()}s")
            }

        return str
    }

    fun onChat(event: ChatEvent, _format: Boolean = false) {
        if (criteria.any { event.matches(it) != null } && time == 0L) {
            time = System.currentTimeMillis()
            val currentSeconds = seconds()

            if (!sendChat || chat == null || currentSeconds == 0L) return

            if (_format) {
                ChatUtils.sendMessage(chat.replace("$1", StringUtils.formatSeconds(currentSeconds)), true)
                return
            }
            ChatUtils.sendMessage(chat.replace("$1", "${currentSeconds}s"), true)
        }

        for (child in children)
            child.onChat(event, _format)
    }

    fun boundToTime(): Long? = boundTo?.time

    fun seconds(): Long {
        val myTime = if (time == 0L) System.currentTimeMillis() else time
        val otherMills = boundToTime()
        val otherTime = if (otherMills == null || otherMills == 0L) System.currentTimeMillis() else otherMills

        return (myTime - otherTime) / 1000
    }

    fun addChild(child: TimerSplitData) = apply {
        if (children.contains(child)) return@apply

        children.add(child)
    }

    @JvmOverloads
    fun addChild(title: String?, criteria: List<Regex>, sendChat: Boolean = true)
            = addChild(TimerSplitData(title, criteria, sendChat))

    @JvmOverloads
    fun addChild(title: String?, criteria: Regex, sendChat: Boolean = true)
            = addChild(title, listOf(criteria), sendChat)
}
