package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.GameUnloadEvent
import com.google.gson.GsonBuilder
import java.io.*
import java.util.concurrent.TimeUnit

abstract class PersistentJson(private val configFile: File) {
    private val afterLoadListeners = mutableListOf<() -> Unit>()
    private val preSaveListeners = mutableListOf<() -> Unit>()

    fun onAfterLoad(cb: () -> Unit) {
        afterLoadListeners.add(cb)
    }

    fun onPreSave(cb: () -> Unit) {
        preSaveListeners.add(cb)
    }

    init {
        EventBus.on<GameUnloadEvent> {
            save()
        }

        Scheduler.schedulePool.scheduleWithFixedDelay(::save, 5L, 5L, TimeUnit.MINUTES)
    }

    abstract fun onLoad(reader: InputStream)
    open fun onLoadDefault() {}

    fun load() {
        if (configFile.exists()) {
            FileInputStream(configFile).use { onLoad(it) }
        } else onLoadDefault()

        afterLoadListeners.forEach { it() }
    }

    abstract fun onSave(writer: OutputStream)

    /**
     * - Saves all the json into a file
     * - Note: must call during game shutdown
     */
    fun save() {
        if (!configFile.parentFile.exists()) configFile.parentFile?.mkdirs()

        preSaveListeners.forEach { it() }

        FileOutputStream(configFile).use { onSave(it) }
    }

    companion object {
        val gson = GsonBuilder().setPrettyPrinting().create()!!
    }
}