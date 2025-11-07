package com.github.synnerz.devonian.api.splits

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.utils.Render2D
import net.minecraft.client.gui.DrawContext

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

    fun register() = event.add()

    fun unregister() = event.remove()

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

    fun str(): String {
        var string = ""

        for (child in children) {
            val title = child.title() ?: continue

            string += "${title.replace("$1", "${child.seconds()}s")}\n"
        }

        return string
    }

    fun defaultStr(): String {
        var string = ""

        for (child in children) {
            val title = child.title() ?: continue

            string += "${title.replace("$1", "15s")}\n"
        }

        return string
    }

    fun draw(ctx: DrawContext, x: Int, y: Int, scale: Float) {
        // If the first child timer is zero we return since we should not be
        // displaying anything at this current time
        if (children.first().time == 0L) return

        Render2D.drawStringNW(ctx, str(), x, y, scale)
    }
}