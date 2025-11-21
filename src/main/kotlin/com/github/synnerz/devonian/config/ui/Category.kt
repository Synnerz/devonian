package com.github.synnerz.devonian.config.ui

import com.github.synnerz.talium.components.*
import com.github.synnerz.talium.effects.OutlineEffect

open class Category(val categoryName: String, val rightPanel: UIBase, leftPanel: UIBase) {
    private val configs = mutableListOf<CategoryData<*>>()
    private val components = mutableListOf<UIRect>()
    private var currentPage = 0
        set(value) {
            field = value.coerceIn(0, components.size / 5)
            onUpdate()
        }
    private val leftArrow = UIRect(1.0, 90.0, 10.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "<-", true).apply {
            setColor(ColorPalette.TEXT_COLOR)
        })
        onMouseRelease { currentPage-- }
    }
    private val rightArrow = UIRect(88.0, 90.0, 10.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "->", true).apply {
            setColor(ColorPalette.TEXT_COLOR)
        })
        onMouseRelease { currentPage++ }
    }
    private var categoryButton: UIRect
    private val categoryTitle = UIText(0.0, 0.0, 100.0, 100.0, categoryName, true).apply {
        setColor(ColorPalette.TEXT_COLOR)
    }

    data class CategoryData<T>(
        val name: String,
        val description: String,
        val type: ConfigType,
        val configData: ConfigData<T>
    )

    init {
        update()
        categoryButton = UIRect(0.0, 12.0 + (9 * ConfigGui.categories.size), 100.0, 8.0, parent = leftPanel).apply {
            onMouseRelease {
                if (ConfigGui.selectedCategory.categoryName == categoryName) return@onMouseRelease
                ConfigGui.selectedCategory.hide()
                ConfigGui.selectedCategory = this@Category
                ConfigGui.selectedCategory.unhide()
                ConfigGui.selectedCategory.update()
            }
            addChild(categoryTitle)
        }
        hide()
    }

    fun addSwitch(
        name: String,
        description: String,
        configData: ConfigData.Switch
    ): ConfigData.Switch {
        configs.add(CategoryData(name, description, ConfigType.SWITCH, configData))
        return configData
    }

    fun <T> addSlider(
        name: String,
        description: String,
        configData: ConfigData.Slider<T>
    ): ConfigData.Slider<T> {
        configs.add(CategoryData(
                name,
                description,
                ConfigType.SLIDER,
                configData
        ))
        return configData
    }

    @JvmOverloads
    fun addButton(
        name: String,
        description: String,
        btnTitle: String = "Click!",
        onClick: () -> Unit
    ) = apply {
        configs.add(CategoryData(
            name, description,
            ConfigType.BUTTON,
            ConfigData.Button(btnTitle, onClick)
        ))
    }

    fun addTextInput(
        name: String,
        description: String,
        configData: ConfigData.TextInput
    ): ConfigData.TextInput {
        configs.add(CategoryData(
            name, description,
            ConfigType.TEXTINPUT,
            configData
        ))
        return configData
    }

    fun addSelection(
        name: String,
        description: String,
        configData: ConfigData.Selection
    ): ConfigData.Selection {
        configs.add(CategoryData(
            name, description,
            ConfigType.SELECTION,
            configData
        ))
        return configData
    }

    fun addColorPicker(
        name: String,
        description: String,
        configData: ConfigData.ColorPicker
    ): ConfigData.ColorPicker {
        configs.add(CategoryData(
            name, description,
            ConfigType.COLORPICKER,
            configData
        ))
        return configData
    }

    fun update() {
        create()
        onUpdate()
    }

    private fun onUpdate() {
        val currentMax = components.size / 5
        when (currentPage) {
            0 -> {
                leftArrow.hide()
                if (currentMax == 0 && configs.isEmpty()) rightArrow.hide()
                else rightArrow.unhide()
            }
            currentMax -> {
                rightArrow.hide()
                leftArrow.unhide()
            }
            else -> {
                leftArrow.unhide()
                rightArrow.unhide()
            }
        }

        for (idx in components.indices) {
            val comp = components[idx]
            val page = idx / 5
            if (page == currentPage) comp.unhide()
            else comp.hide()
        }
    }

    @Suppress("unchecked_cast")
    private fun create() {
        var i = 0

        while (configs.isNotEmpty()) {
            val data = configs.removeFirst()
            val y = 1 + (i % 5) * 17
            components.add(createBase(y.toDouble(), rightPanel).apply {
                addChild(createTitle(data.name))
                addChild(createDescription(data.description))
                addChild(
                    when (data.type) {
                        ConfigType.SWITCH -> createSwitch(data.configData as ConfigData.Switch)
                        ConfigType.SLIDER -> createSlider(data.configData as ConfigData.Slider<Double>)
                        ConfigType.BUTTON -> createButton(data.configData as ConfigData.Button)
                        ConfigType.TEXTINPUT -> createTextInput(data.configData as ConfigData.TextInput)
                        ConfigType.SELECTION -> createSelection(data.configData as ConfigData.Selection)
                        ConfigType.COLORPICKER -> createColorPicker(data.configData as ConfigData.ColorPicker)
                    }
                )
                hide()
            })
            i++
        }
    }

    fun hide() {
        categoryTitle.setColor(ColorPalette.TEXT_COLOR)
        leftArrow.hide()
        rightArrow.hide()
        for (comp in components)
            comp.hide()
    }

    fun unhide() {
        categoryTitle.setColor(ColorPalette.LIGHT_TEXT_COLOR)
        leftArrow.unhide()
        rightArrow.unhide()
        onUpdate()
    }

    private fun createBase(y: Double, parent: UIBase): UIRect =
        UIRect(1.0, y, 98.0, 15.0, parent = parent).apply {
            addEffects(OutlineEffect(1.0, ColorPalette.OUTLINE_COLOR))
        }

    private fun createTitle(text: String, parent: UIRect? = null): UIText =
        UIText(0.0, 2.0, 100.0, 25.0, text, true, parent).apply {
            setColor(ColorPalette.TEXT_COLOR)
        }

    private fun createDescription(text: String, parent: UIRect? = null): UIWrappedText =
        UIWrappedText(2.0, 28.0, 75.0, 75.0, text, parent = parent).apply {
            setColor(ColorPalette.LIGHT_TEXT_COLOR)
            textScale = 0.9f
        }

    private fun createButton(
        configData: ConfigData.Button,
        parent: UIRect? = null
    ): UIRect = UIRect(80.0, 25.0, 15.0, 50.0, parent = parent).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(UIText(0.0, 0.0, 100.0, 100.0, configData.btnTitle, true).apply {
            setColor(ColorPalette.TEXT_COLOR)
        })
        onMouseRelease {
            configData.onClick()
        }
    }

    private fun createSwitch(configData: ConfigData.Switch, parent: UIRect? = null): UISwitch {
        return UISwitch(80.0, 25.0, 15.0, 50.0, configData.get(), parent = parent).apply {
            setColor(ColorPalette.TERTIARY_COLOR)
            knob = UIKnobSwitch(85.0)
            knob.enabledColor = ColorPalette.ENABLED_COLOR
            knob.disabledColor = ColorPalette.DISABLED_COLOR
            onMouseRelease {
                configData.set(state)
            }

            configData.onChange { state = configData.get() }
        }
    }

    private fun createSlider(
        configData: ConfigData.Slider<Double>,
        parent: UIRect? = null
    ): UISlider = object : UISlider(80.0, 25.0, 15.0, 50.0, configData.get(), configData.min, configData.max, parent = parent) {
        override fun setCurrentX(x: Double) {
            super.setCurrentX(x)
            configData.set(this.value)
        }

        override fun setCurrentValue(value: Double) {
            super.setCurrentValue(value)
            configData.set(this.value)
        }
    }.apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        configData.onChange {
            value = configData.get()
        }
    }

    private fun createTextInput(
        configData: ConfigData.TextInput,
        parent: UIRect? = null
    ): UITextInput = UITextInput(80.0, 25.0, 15.0, 50.0, configData.get(), parent = parent).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        onKeyType {
            configData.set(text)
        }
        configData.onChange {
            text = configData.get()
        }
    }

    private fun createSelection(
        configData: ConfigData.Selection,
        parent: UIRect? = null
    ): UISelection = object : UISelection(80.0, 25.0, 15.0, 50.0, configData.get(), configData.options, parent = parent) {
        override fun setOption(idx: Int) {
            super.setOption(idx)
            configData.set(value)
        }
    }.apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        configData.onChange {
            value = configData.get().coerceIn(0, options.lastIndex)
            centerText.text = options[value]
        }
    }

    // TODO: impl me
    private fun createColorPicker(
        configData: ConfigData.ColorPicker,
        parent: UIRect? = null
    ): UIRect = UIRect(80.0, 25.0, 15.0, 50.0).apply { setColor(ColorPalette.TERTIARY_COLOR) }
}