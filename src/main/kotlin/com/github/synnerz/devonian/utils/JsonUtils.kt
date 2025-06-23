package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.events.EventBus
import com.github.synnerz.devonian.events.GameUnloadEvent
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
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
    val listeners = mutableListOf<() -> Unit>()

    init {
        EventBus.on<GameUnloadEvent> { save() }
    }

    fun afterLoad(cb: () -> Unit) {
        listeners.add(cb)
    }

    /**
     * - Sets a value to the specified name key
     */
    fun <T> set(name: String, value: T): JsonUtils {
        when (value) {
            is Boolean -> json.addProperty(name, value)
            is String -> json.addProperty(name, value)
            is Char -> json.addProperty(name, value)
            is Number -> json.addProperty(name, value)
            is JsonArray -> json.add(name, value)
        }
        return this
    }

    /**
     * - Sets a boolean value to the specified name key
     */
    fun setConfig(name: String, value: Boolean): JsonUtils {
        if (json.get("config") == null) {
            json.add("config", JsonObject())
        }
        json.get("config").asJsonObject.addProperty(name, value)
        return this
    }

    /**
     * - Gets a value from the specified name key
     */
    inline fun <reified T> get(name: String): T? {
        val value = json.get(name) ?: return null

        return when (T::class) {
            String::class -> value.asString as T
            Int::class -> value.asInt as T
            Double::class -> value.asDouble as T
            Float::class -> value.asFloat as T
            Long::class -> value.asLong as T
            Boolean::class -> value.asBoolean as T
            List::class -> value.asJsonArray.toList().map { it.asString } as T
            else -> null
        }
    }

    /**
     * - Gets a boolean value from the specified name key
     */
    fun getConfig(name: String): Boolean? = json.get("config")?.asJsonObject?.get(name)?.asBoolean

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
                    if (k == "config") {
                        val savedConfig = v.asJsonObject
                        for (k2 in savedConfig.keySet()) {
                            val feat = Devonian.features.find { feat -> feat.configName == k2 }
                            val prev = getConfig(k2)

                            if (feat !== null && prev !== null) {
                                // Since every feature starts at `false` this does not really matter
                                // future note: if any feature changes to `true` as default, make this account
                                // for that specific scenario and actually trigger the `Feature`
                                if (prev == savedConfig.get(k2).asBoolean) continue
                                feat.onToggle(!prev)
                            }
                        }
                    }

                    json.add(k, v)
                }

                for (cb in listeners)
                    cb()
            }
            return
        }

        json = JsonObject()
        for (cb in listeners)
            cb()
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