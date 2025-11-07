package com.github.synnerz.devonian.api.splits

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent

data class TimerSplitData(
    val title: String? = null,
    val criteria: List<Regex>,
    val sendChat: Boolean = true,
    var boundTo: TimerSplitData? = null,
    var time: Long = 0L
) {
    constructor(title: String?, criteria: Regex, sendChat: Boolean = true): this(title, listOf(criteria), sendChat)

    fun onChat(event: ChatEvent) {
        if (criteria.any { event.matches(it) != null } && time == 0L) {
            time = System.currentTimeMillis()

            if (!sendChat || title == null) return

            ChatUtils.sendMessage(title.replace("$1", "${seconds()}s"), true)
        }
    }

    fun boundToTime(): Long? = boundTo?.time

    fun seconds(): Long {
        val myTime = if (time == 0L) System.currentTimeMillis() else time
        val otherMills = boundToTime()
        val otherTime = if (otherMills == null || otherMills == 0L) System.currentTimeMillis() else otherMills

        return (myTime - otherTime) / 1000
    }
}
