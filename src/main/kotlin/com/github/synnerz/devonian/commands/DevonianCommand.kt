package com.github.synnerz.devonian.commands

object DevonianCommand {
    private val commandListeners = mutableListOf<() -> Int>()
    val command = BaseCommand("devonian") {
        for (cb in commandListeners) {
            val res = cb()
            if (res != 1) return@BaseCommand res
        }
        1
    }

    fun initialize() {
        command.register()
    }

    fun onRun(cb: () -> Int) {
        commandListeners.add(cb)
    }
}