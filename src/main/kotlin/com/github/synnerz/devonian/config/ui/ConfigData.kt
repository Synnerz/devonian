package com.github.synnerz.devonian.config.ui

import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.utils.StringUtils.camelCaseToSentence
import java.awt.Color

open class ConfigData<T>(
    val configName: String?,
    val type: ConfigType,
    var value: T,
    description: String? = null,
    displayName: String? = null,
    val subcategory: String = "General",
) {
    val description = description ?: ""
    val displayName = displayName ?: configName?.camelCaseToSentence() ?: "Unnamed Button"

    private val onChangeHook = mutableListOf<((T) -> Unit)>()
    open fun get() = value

    open fun set(newVal: T) {
        if (configName == null) return
        value = newVal
        Config.setConfig(configName, newVal)
        onChangeHook.forEach { it(newVal) }
    }

    init {
        if (configName != null) {
            Config.setConfig(configName, value)

            Config.onAfterLoad {
                val savedValue = Config.getConfig(configName, value) ?: return@onAfterLoad
                set(savedValue)
            }
        }
    }

    fun onChange(cb: (T) -> Unit) {
        onChangeHook.add(cb)
    }

    open class Switch(
        configName: String,
        value: Boolean,
        description: String? = null,
        displayName: String? = null,
        val isHidden: Boolean = false,
        subcategory: String = "General",
    ) : ConfigData<Boolean>(configName, ConfigType.SWITCH, value, description, displayName, subcategory)

    class FeatureSwitch(
        configName: String,
        value: Boolean,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
    ) : Switch(
        configName,
        value,
        description,
        displayName,
        subcategory = subcategory,
    ) {
        val subconfigs = mutableListOf<ConfigData<*>>()
    }

    class Slider<T : Number>(
        configName: String,
        value: T,
        val min: Double,
        val max: Double,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
    ) : ConfigData<T>(configName, ConfigType.SLIDER, value, description, displayName, subcategory)

    class DecimalSlider<T : Number>(
        configName: String,
        value: T,
        val min: Double,
        val max: Double,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
    ) : ConfigData<T>(configName, ConfigType.DECIMALSLIDER, value, description, displayName, subcategory)

    class Button(
        val onClick: () -> Unit,
        val btnTitle: String = "Click!",
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
    ) : ConfigData<Unit>(null, ConfigType.BUTTON, Unit, description, displayName, subcategory)

    class TextInput(
        configName: String,
        value: String,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
    ) : ConfigData<String>(configName, ConfigType.TEXTINPUT, value, description, displayName, subcategory)

    class Selection(
        configName: String,
        value: Int,
        val options: List<String>,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
    ) : ConfigData<Int>(configName, ConfigType.SELECTION, value, description, displayName, subcategory) {
        fun getCurrent(): String = options[get()]
    }

    class ColorPicker(
        configName: String,
        value: Int,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
    ) : ConfigData<Int>(configName, ConfigType.COLORPICKER, value, description, displayName, subcategory) {
        private var color = Color(value, true)
        fun getColor(): Color = color

        override fun set(newVal: Int) {
            super.set(newVal)
            color = Color(newVal, true)
        }
    }
}
