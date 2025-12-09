package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ui.ConfigData
import com.github.synnerz.devonian.config.ui.ConfigType
import com.github.synnerz.talium.components.*
import com.github.synnerz.talium.effects.OutlineEffect
import com.github.synnerz.talium.events.UIClickEvent
import com.github.synnerz.talium.events.UIFocusEvent
import com.github.synnerz.talium.events.UIKeyType

class Category(val categoryName: String, val rightPanel: UIBase, leftPanel: UIBase) {
    private val configs = Config.categories[categoryName]!!.toMutableList()
    private val components = mutableListOf<UIRect>()
    private val colorComponents = mutableListOf<UIColorPicker>()
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
        onMouseRelease {
            if (!canTrigger()) return@onMouseRelease
            if (it.button != 0) return@onMouseRelease
            currentPage--
        }
    }
    private val rightArrow = UIRect(88.0, 90.0, 10.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "->", true).apply {
            setColor(ColorPalette.TEXT_COLOR)
        })
        onMouseRelease {
            if (!canTrigger()) return@onMouseRelease
            if (it.button != 0) return@onMouseRelease
            currentPage++
        }
    }
    private var categoryButton: UIRect
    private val categoryTitle = UIText(0.0, 0.0, 100.0, 100.0, categoryName, true).apply {
        setColor(ColorPalette.TEXT_COLOR)
    }

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

    // shit workaround to prevent the player from opening
    // other things while changing the color of the
    // color gradient
    fun canTrigger(): Boolean {
        return !colorComponents.any { it.arrowToggle }
    }

    fun hideColorPickers() {
        for (comp in colorComponents)
            if (comp.arrowToggle)
                comp.hideDropdown()
    }

    fun onMouseScroll(delta: Int) {
        if (delta == -1) {
            currentPage++
            return
        }

        currentPage--
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
                addChild(createTitle(data.displayName))
                addChild(createDescription(data.description))
                addChild(
                    when (data.type) {
                        ConfigType.SWITCH -> createSwitch(data as ConfigData.Switch)
                        ConfigType.SLIDER -> createSlider(data as ConfigData.Slider<Double>)
                        ConfigType.DECIMALSLIDER -> createDecimalSlider(data as ConfigData.DecimalSlider<Double>)
                        ConfigType.BUTTON -> createButton(data as ConfigData.Button)
                        ConfigType.TEXTINPUT -> createTextInput(data as ConfigData.TextInput)
                        ConfigType.SELECTION -> createSelection(data as ConfigData.Selection)
                        ConfigType.COLORPICKER -> createColorPicker(data as ConfigData.ColorPicker, this@apply)
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
            if (!canTrigger()) return@onMouseRelease
            configData.onClick()
        }
    }

    private fun createSwitch(configData: ConfigData.Switch, parent: UIRect? = null): UISwitch {
        return object : UISwitch(80.0, 25.0, 15.0, 50.0, configData.get(), parent = parent) {
            override fun onMouseRelease(event: UIClickEvent) = apply {
                if (!canTrigger()) return@apply
                super.onMouseRelease(event)
            }
        }.apply {
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
            if (!canTrigger()) return
            super.setCurrentX(x)
            configData.set(this.value)
        }

        override fun setCurrentValue(value: Double) {
            if (!canTrigger()) return
            super.setCurrentValue(value)
            configData.set(this.value)
        }
    }.apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        configData.onChange {
            value = configData.get()
        }
    }

    private fun createDecimalSlider(
        configData: ConfigData.DecimalSlider<Double>,
        parent: UIRect? = null
    ): UIDecimalSlider = object : UIDecimalSlider(80.0, 25.0, 15.0, 50.0, configData.get(), configData.min, configData.max, parent = parent) {
        override fun setCurrentX(x: Double) {
            if (!canTrigger()) return
            super.setCurrentX(x)
            configData.set(getCurrentValue())
        }

        override fun setCurrentValue(value: Double) {
            if (!canTrigger()) return
            super.setCurrentValue(value)
            configData.set(getCurrentValue())
        }
    }.apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        configData.onChange {
            value = configData.get() * 100
        }
    }

    private fun createTextInput(
        configData: ConfigData.TextInput,
        parent: UIRect? = null
    ): UITextInput = object : UITextInput(80.0, 25.0, 15.0, 50.0, configData.get(), parent = parent) {
        override fun onFocus(event: UIFocusEvent) = apply {
            if (!canTrigger()) {
                focused = false
                return@apply
            }
            super.onFocus(event)
        }

        override fun onKeyType(event: UIKeyType) = apply {
            if (!canTrigger()) return@apply
            super.onKeyType(event)
        }
    }.apply {
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
            if (!canTrigger()) return
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

    private fun createColorPicker(
        configData: ConfigData.ColorPicker,
        parent: UIRect? = null
    ): UIColorPicker = object : UIColorPicker(80.0, 25.0, 15.0, 50.0, configData.get(), parent) {
        init {
            if (rightPanel.parent?.parent != null)
                fakeChild.setChildOf(rightPanel.parent!!.parent!!)
            fakeChild._height = 15.0
            fakeChild._width = 15.0
            huePicker.parent = fakeChild
            gradientPicker.parent = fakeChild

            colorComponents.add(this)
        }

        override fun onUpdate() = apply {
            fakeChild._x = (x / (fakeChild.parent?.width ?: 1.0)) * 100
            fakeChild._y = 5.0 + (y / (fakeChild.parent?.height ?: 1.0)) * 100
            fakeChild.setDirty()
        }

        override fun setValue(hue: Double) {
            super.setValue(hue)
            configData.set(value)
        }

        override fun unhideDropdown() {
            if (!canTrigger()) return
            hideColorPickers()
            super.unhideDropdown()
        }
    }.apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        configData.onChange {
            setRgb(configData.get())
        }
    }
}