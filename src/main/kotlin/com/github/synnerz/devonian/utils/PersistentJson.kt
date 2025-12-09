package com.github.synnerz.devonian.utils

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

abstract class PersistentJson(private val configFile: File) {
    private val afterLoadListeners = mutableListOf<() -> Unit>()
    private val preSaveListeners = mutableListOf<() -> Unit>()

    fun onAfterLoad(cb: () -> Unit) {
        afterLoadListeners.add(cb)
    }

    fun onPreSave(cb: () -> Unit) {
        preSaveListeners.add(cb)
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