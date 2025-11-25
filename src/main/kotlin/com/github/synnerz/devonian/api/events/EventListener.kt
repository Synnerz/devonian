package com.github.synnerz.devonian.api.events

import com.github.synnerz.devonian.utils.State
import com.github.synnerz.devonian.utils.Toggleable
import kotlin.reflect.KClass

class EventListener<T : Event> (
    private val cb: (T) -> Unit,
    private val event: KClass<T>
) : Toggleable() {
    @Volatile private var isRegistered = false
    @Volatile private var isActuallyRegistered = false
    private var enabledState: State<Boolean>? = null

    @Suppress("UNCHECKED_CAST")
    override fun add() {
        EventBus.add(event, this as EventListener<Event>)
        isActuallyRegistered = true
    }

    @Suppress("UNCHECKED_CAST")
    override fun remove() {
        EventBus.remove(event, this as EventListener<Event>)
        isActuallyRegistered = false
    }

    fun trigger(event: T) {
        cb(event)
    }
}