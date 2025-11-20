package com.github.synnerz.devonian.config.ui

import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.JsonUtils
import java.awt.Color

open class ConfigData<T>(
    val configName: String?,
    var value: T
) {
    private var onChangeHook: (() -> Unit)? = null
    open fun get() = value

    open fun set(newVal: T) {
        if (configName == null) return
        value = newVal
        JsonUtils.setConfig(configName, newVal)
        onChangeHook?.let { it() }
    }

    init {
        if (configName != null)
            JsonUtils.setConfig(configName, value)

        JsonUtils.afterLoad {
            if (configName == null) return@afterLoad
            val jsonVal = JsonUtils.fromConfig<T>(configName) ?: return@afterLoad
            set(jsonVal)
        }
    }

    fun onChange(cb: () -> Unit) {
        onChangeHook = cb
    }

    open class Switch(
        configName: String,
        value: Boolean
    ) : ConfigData<Boolean>(configName, value)

    class FeatureSwitch(
        configName: String,
        value: Boolean,
        val feature: Feature
    ) : Switch(configName, value) {
        override fun set(newVal: Boolean) {
            super.set(newVal)
            feature.onToggle(newVal)
        }
    }

    class Slider<T>(
        configName: String,
        value: T,
        val min: Double,
        val max: Double
    ) : ConfigData<T>(configName, value)

    class Button(
        val btnTitle: String,
        val onClick: () -> Unit
    ) : ConfigData<Boolean>(null, false)

    class TextInput(
        configName: String,
        value: String
    ) : ConfigData<String>(configName, value)

    class Selection(
        configName: String,
        value: Int,
        val options: List<String>
    ) : ConfigData<Int>(configName, value) {
        fun getCurrent(): String = options[get()]
    }

    class ColorPicker(
        configName: String,
        value: Int
    ) : ConfigData<Int>(configName, value) {
        private var color = Color(value, true)
        fun getColor(): Color = color

        override fun set(newVal: Int) {
            super.set(newVal)
            color = Color(newVal)
        }
    }
}
