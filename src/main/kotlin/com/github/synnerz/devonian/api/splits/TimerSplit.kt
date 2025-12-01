package com.github.synnerz.devonian.api.splits

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.utils.StringUtils

class TimerSplit @JvmOverloads constructor(
    val children: MutableList<TimerSplitData> = mutableListOf()
) {
    constructor(vararg data: TimerSplitData): this(data.toMutableList())

    val event = EventBus.on<ChatEvent>({ event ->
        for (child in children)
            child.onChat(event)
    }, false)

    init {
        findBounds()
    }

    private fun findBounds() {
        for (idx in children.indices) {
            if (idx == 0) continue
            val parent = children[idx - 1]
            val data = children[idx]

            if (data.boundTo == null)
                data.boundTo = parent

            for (child in children[idx].children) {
                child.boundTo = data.boundTo
            }
        }
    }

    fun onChat(event: ChatEvent, _format: Boolean = false) {
        for (child in children)
            child.onChat(event, _format)
    }

    fun reset() {
        for (child in children) {
            child.time = 0L
            for (subChild in child.children)
                subChild.time = 0L
        }
    }

    fun addChild(child: TimerSplitData) = apply {
        if (children.contains(child)) return@apply

        children.add(child)
        findBounds()
    }

    @JvmOverloads
    fun addChild(title: String?, criteria: List<Regex>, sendChat: Boolean = true)
        = addChild(TimerSplitData(title, criteria, sendChat))

    @JvmOverloads
    fun addChild(title: String?, criteria: Regex, sendChat: Boolean = true)
        = addChild(title, listOf(criteria), sendChat)

    fun str(_format: Boolean = false): List<String> = children.mapNotNull { c ->
        if (_format) return@mapNotNull c.title(_format)?.replace("$1", StringUtils.formatSeconds(c.seconds()))
        c.title()?.replace("$1", "${c.seconds()}s")
    }

    fun defaultStr(_format: Boolean = false): List<String> = children.mapNotNull { c ->
        if (_format) return@mapNotNull c.title(_format)?.replace("$1", StringUtils.formatSeconds(c.seconds()))
        c.title()?.replace("$1", "${c.seconds()}s")
    }
}