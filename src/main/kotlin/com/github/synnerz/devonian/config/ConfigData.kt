package com.github.synnerz.devonian.config

import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils.camelCaseToSentence
import java.awt.Color
import kotlin.math.max

open class ConfigData<T>(
    val configName: String?,
    val type: ConfigType,
    var value: T,
    val parent: FeatureSwitch?,
    description: String? = null,
    displayName: String? = null,
    val subcategory: String = "General",
    searchTags: Set<String> = emptySet(),
    val isHidden: Boolean = false,
) {
    val description = description ?: ""
    val displayName = displayName ?: configName?.camelCaseToSentence() ?: "Unnamed Button"
    val searchTags = buildMap {
        fun add(k: String, v: Int) {
            val k = k.lowercase().replace(searchStripReg, "")
            if (k.isEmpty()) return
            merge(k, v, ::max)
        }
        searchTags.forEach { add(it, 3) }
        this@ConfigData.displayName.split(searchSplitReg).forEach { add(it, 5) }
        this@ConfigData.description.split(searchSplitReg).forEach { add(it, 1) }
    }

    val state = BasicState(value)

    private val onChangeHook = mutableListOf<((T) -> Unit)>()
    open fun get() = value

    open fun set(newVal: T) {
        if (configName == null) return
        value = newVal
        Config.setConfig(configName, newVal)
        state.value = newVal
        onChangeHook.forEach { it(newVal) }
    }

    init {
        if (configName != null) {
            Config.setConfig(configName, value)

            Config.onAfterLoad {
                val savedValue = Config.getConfig(configName, value) ?: return@onAfterLoad
                set(savedValue)
            }

            Config.onPreSave {
                Config.setConfig(configName, value)
            }
        }
    }

    fun onChange(cb: (T) -> Unit) {
        onChangeHook.add(cb)
    }

    open class Switch(
        configName: String,
        value: Boolean,
        parent: FeatureSwitch?,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
        searchTags: Set<String> = emptySet(),
        isHidden: Boolean = false,
    ) : ConfigData<Boolean>(
        configName,
        ConfigType.SWITCH,
        value,
        parent,
        description,
        displayName,
        subcategory,
        searchTags,
        isHidden,
    )

    class FeatureSwitch(
        configName: String,
        value: Boolean,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
        searchTags: Set<String> = emptySet(),
        isHidden: Boolean = false,
    ) : Switch(
        configName,
        value,
        null,
        description,
        displayName,
        subcategory,
        searchTags,
        isHidden,
    ) {
        val subconfigs = mutableListOf<ConfigData<*>>()
    }

    class Slider<T : Number>(
        configName: String,
        value: T,
        parent: FeatureSwitch?,
        val min: Double,
        val max: Double,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
        searchTags: Set<String> = emptySet(),
        isHidden: Boolean = false,
    ) : ConfigData<T>(
        configName,
        ConfigType.SLIDER,
        value,
        parent,
        description,
        displayName,
        subcategory,
        searchTags,
        isHidden
    )

    class DecimalSlider<T : Number>(
        configName: String,
        value: T,
        parent: FeatureSwitch?,
        val min: Double,
        val max: Double,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
        searchTags: Set<String> = emptySet(),
        isHidden: Boolean = false,
    ) : ConfigData<T>(
        configName,
        ConfigType.DECIMALSLIDER,
        value,
        parent,
        description,
        displayName,
        subcategory,
        searchTags,
        isHidden,
    )

    class Button(
        val onClick: () -> Unit,
        val btnTitle: String = "Click!",
        parent: FeatureSwitch?,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
        searchTags: Set<String> = emptySet(),
        isHidden: Boolean = false,
    ) : ConfigData<Unit>(
        null,
        ConfigType.BUTTON,
        Unit,
        parent,
        description,
        displayName,
        subcategory,
        searchTags,
        isHidden
    )

    class TextInput(
        configName: String,
        value: String,
        parent: FeatureSwitch?,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
        searchTags: Set<String> = emptySet(),
        isHidden: Boolean = false,
    ) : ConfigData<String>(
        configName,
        ConfigType.TEXTINPUT,
        value,
        parent,
        description,
        displayName,
        subcategory,
        searchTags,
        isHidden,
    )

    class Selection(
        configName: String,
        value: Int,
        val options: List<String>,
        parent: FeatureSwitch?,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
        searchTags: Set<String> = emptySet(),
        isHidden: Boolean = false,
    ) : ConfigData<Int>(
        configName,
        ConfigType.SELECTION,
        value,
        parent,
        description,
        displayName,
        subcategory,
        searchTags,
        isHidden,
    ) {
        fun getCurrent(): String = options[get()]
    }

    class ColorPicker(
        configName: String,
        value: Int,
        parent: FeatureSwitch?,
        description: String? = null,
        displayName: String? = null,
        subcategory: String = "General",
        searchTags: Set<String> = emptySet(),
        isHidden: Boolean = false,
    ) : ConfigData<Int>(
        configName,
        ConfigType.COLORPICKER,
        value,
        parent,
        description,
        displayName,
        subcategory,
        searchTags,
        isHidden,
    ) {
        private var color = Color(value, true)
        fun getColor(): Color = color

        override fun set(newVal: Int) {
            super.set(newVal)
            color = Color(newVal, true)
        }
    }

    companion object {
        val searchSplitReg = "[ /]".toRegex()
        val searchStripReg = "[^a-z0-9]".toRegex(RegexOption.IGNORE_CASE)
    }
}
