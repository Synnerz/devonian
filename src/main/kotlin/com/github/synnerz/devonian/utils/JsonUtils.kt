package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.events.Events
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.MinecraftClient
import java.io.File
import java.io.FileReader
import java.io.FileWriter

// TODO: add auto backup type system

object JsonUtils {
    val configFile = File(
        MinecraftClient.getInstance().runDirectory,
        "config"
    ).resolve("devonianConfig.json")
    val gson = GsonBuilder().setPrettyPrinting().create()
    var json = JsonObject()

    init {
        Events.onGameUnload { save() }
    }

    /**
     * - Sets a boolean value to the specified name key
     */
    fun set(name: String, value: Boolean): JsonUtils {
        json.addProperty(name, value)
        return this
    }

    /**
     * - Gets a boolean value from the specified name key
     */
    fun get(name: String): Boolean? = json.get(name)?.asBoolean

    /**
     * - Loads the data from the saved file if it exists
     * - Note: this should run once every other feature is done loading.
     * it's done this way so that it respects the default values.
     */
    fun load() {
        if (configFile.exists()) {
            FileReader(configFile).use {
                val savedData = JsonParser.parseReader(it).asJsonObject
                if (savedData.isEmpty) return
                for ((k, v) in savedData.entrySet()) {
                    json.add(k, v)
                }
            }
            return
        }

        json = JsonObject()
    }

    /**
     * - Saves all the json into a file
     * - Note: must call during game shutdown
     */
    fun save() {
        if (!configFile.parentFile.exists()) configFile.parentFile?.mkdirs()
        FileWriter(configFile).use { gson.toJson(json, it) }
    }
}