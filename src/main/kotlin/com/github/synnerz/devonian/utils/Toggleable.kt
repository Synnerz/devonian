package com.github.synnerz.devonian.utils

abstract class Toggleable {
    @Volatile private var isRegistered = false
    @Volatile private var isActuallyRegistered = false
    private var enabledState: State<Boolean>? = null

    protected abstract fun add()
    protected abstract fun remove()
    protected open fun change() {}

    fun register() = apply {
        if (isRegistered) return@apply
        if (!isActuallyRegistered && (enabledState?.value ?: true)) {
            add()
            change()
            isActuallyRegistered = true
        }
        isRegistered = true
    }

    fun unregister() = apply {
        if (!isRegistered) return@apply
        if (isActuallyRegistered) {
            remove()
            change()
            isActuallyRegistered = false
        }
        isRegistered = false
    }

    fun isRegistered() = isRegistered

    fun setRegistered(b: Boolean) = apply {
        if (b) register()
        else unregister()
    }

    private fun update(b: Boolean) {
        if (!isRegistered) return
        if (b) {
            if (!isActuallyRegistered) {
                add()
                change()
                isActuallyRegistered = true
            }
        } else {
            if (isActuallyRegistered) {
                remove()
                change()
                isActuallyRegistered = false
            }
        }
    }

    fun setEnabled(state: State<Boolean>) = apply {
        if (enabledState != null) throw IllegalStateException("can only setEnabled once")
        enabledState = state
        enabledState?.listen(::update)
        enabledState?.value?.let { update(it) }
    }
}