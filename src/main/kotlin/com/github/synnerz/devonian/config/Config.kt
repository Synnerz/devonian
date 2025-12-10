package com.github.synnerz.devonian.config

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.config.json.PersistentJsonData
import com.github.synnerz.devonian.config.ui.ConfigData
import com.github.synnerz.devonian.hud.texthud.DataProvider
import java.io.File

val configFile = File(
    Devonian.minecraft.gameDirectory,
    "config"
).resolve("devonianConfig.json")

val jsonLoader = PersistentJsonData(configFile)

object Config : PersistentData by jsonLoader {
    val configRoot: DataObject
        get() = getData().getObject("config")
    val hudRoot: DataObject
        get() = getData().getObject("huds")

    fun <T> set(key: String, value: T) = apply { getData().set(key, value) }
    inline fun <reified T> get(key: String) = getData().get<T>(key)

    fun <T> setConfig(name: String, value: T) = apply { configRoot.set(name, value) }
    inline fun <reified T> getConfig(name: String): T? = configRoot.get<T>(name)
    fun <T> getConfig(name: String, value: T): T? = configRoot.get(name, value)

    fun getHud(name: String): NullableHudData {
        val obj = hudRoot.getObject(name)
        return NullableHudData(
            obj.getDouble("x"),
            obj.getDouble("y"),
            obj.getFloat("scale"),
            obj.getInt("anchor"),
            obj.getInt("align"),
            obj.getBoolean("shadow"),
            obj.getInt("backdrop"),
        )
    }

    fun setHud(name: String, value: NullableHudData) = apply {
        val obj = hudRoot.getObject(name)

        value.x?.let { obj.set("x", it) }
        value.y?.let { obj.set("y", it) }
        value.scale?.let { obj.set("scale", it) }
        value.anchor?.let { obj.set("anchor", it) }
        value.align?.let { obj.set("align", it) }
        value.shadow?.let { obj.set("shadow", it) }
        value.backdrop?.let { obj.set("backdrop", it) }
    }

    val categories = listOf(
        "Dungeons",
        "Dungeon Map",
        "Garden",
        "Slayers",
        "End",
        "Diana",
        "Misc",
    ).associateWith { mutableListOf<ConfigData<*>>() }
    val categoryFromConfig = mutableMapOf<ConfigData<*>, String>()

    fun registerCategory(config: ConfigData<*>, category: String) {
        categories[category]!!.add(config)
        categoryFromConfig[config] = category
    }

    val features = mutableListOf<ConfigData.FeatureSwitch>()
}

data class NullableHudData(
    var x: Double? = null,
    var y: Double? = null,
    var scale: Float? = null,
    var anchor: Int? = null,
    var align: Int? = null,
    var shadow: Boolean? = null,
    var backdrop: Int? = null,
) {
    companion object {
        fun from(data: DataProvider) = NullableHudData(
            data.x,
            data.y,
            data.scale,
            data.anchor.ordinal,
            data.align.ordinal,
            data.shadow,
            data.backdrop.ordinal,
        )
    }
}