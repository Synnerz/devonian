package com.github.synnerz.devonian.api.splits

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EventBus

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
            children[idx].boundTo = children[idx - 1]
            for (child in children[idx].children)
                child.boundTo = children[idx - 1]
        }
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

    fun str(): List<String> = children.mapNotNull { c -> c.title()?.replace("$1", "${c.seconds()}s") }

    fun defaultStr(): List<String> = children.mapNotNull { c -> c.title()?.replace("$1", "${c.seconds()}s") }
}