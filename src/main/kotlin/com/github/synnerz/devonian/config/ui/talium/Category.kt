package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.devonian.config.ConfigType
import com.github.synnerz.talium.components.*
import com.github.synnerz.talium.effects.OutlineEffect
import com.github.synnerz.talium.events.UIClickEvent
import com.github.synnerz.talium.events.UIFocusEvent
import com.github.synnerz.talium.events.UIKeyType

class Category(
    val category: Categories,
    val rightPanel: UIBase,
    leftPanel: UIBase,
    idx: Int,
    val createBtn: Boolean = true,
) {
    val configs = Config.categories[category]!!
    private val components = mutableListOf<UIRect>()
    private val colorComponents = mutableListOf<UIColorPicker>()
    private val subcategoryPanel = UIRect(0.0, 0.0, 100.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
    }
    private val scrollableRect = UIScrollable(0.0, 9.0, 100.0, 81.0, parent = rightPanel)
    private val subcategoriesRect = mutableMapOf<String, UIScrollable>()
    private var categoryButton: UIRect?
    private val categoryTitle = UIText(0.0, 0.0, 100.0, 100.0, category.displayName, true).apply {
        setColor(ColorPalette.TEXT_COLOR)
    }
    private var currentSubcategory = category.subcategories[0]

    init {
        val subCount = category.subcategories.size
        val subCatGap = 0.5
        val subCatMargins = 1.0
        val subCatWidth = (100.0 - subCatMargins * 2 - subCatGap * (subCount - 1)) / subCount
        category.subcategories.forEachIndexed { jdx, name ->
            subcategoriesRect[name] = UIScrollable(0.0, 9.0, 100.0, 81.0, parent = rightPanel).apply { hide() }
            subcategoryPanel.addChild(
                UIRect(subCatMargins + (subCatWidth + subCatGap) * jdx, 2.5, subCatWidth, 95.0).apply {
                    val text = UIText(0.0, 0.0, 100.0, 100.0, name, true, parent = this).apply {
                        setColor(ColorPalette.TEXT_COLOR)
                        // TODO: add a way to make the current sub category's text different color
                    }
                    onMouseRelease { event ->
                        if (event.button != 0) return@onMouseRelease
                        subcategoriesRect[currentSubcategory]?.hide()
                        hideColorPickers()
                        currentSubcategory = name
                        subcategoriesRect[currentSubcategory]?.unhide()
                    }
                    addEffect(OutlineEffect(0.5, ColorPalette.OUTLINE_COLOR))
                }
            )
        }
        create()

        if (createBtn) {
            categoryButton = UIRect(
                0.0, 12.0 + 9 * idx,
                100.0, 8.0,
                parent = leftPanel
            ).apply {
                onMouseRelease {
                    if (ConfigGui.selectedCategory === this@Category) return@onMouseRelease
                    ConfigGui.selectedCategory.hide()
                    ConfigGui.selectedCategory = this@Category
                    ConfigGui.selectedCategory.unhide()
                }
                addChild(categoryTitle)
            }
        }
        else categoryButton = null

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

    @Suppress("unchecked_cast")
    private fun create() {
        configs.forEach { (subname, list) ->
            val scrollable = subcategoriesRect[subname]!!

            var idx = 0
            list.forEach { data ->
                if (data.isHidden && !Devonian.isDev) return@forEach
                val i = idx++

                val y = 1 + i * 17.0

                components.add(createBase(y, scrollable).apply {
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
                })
            }
        }
    }

    fun hide() {
        categoryTitle.setColor(ColorPalette.TEXT_COLOR)
        scrollableRect.hide()
        subcategoryPanel.hide()
        subcategoriesRect[currentSubcategory]?.hide()
    }

    fun unhide() {
        categoryTitle.setColor(ColorPalette.LIGHT_TEXT_COLOR)
        scrollableRect.unhide()
        subcategoryPanel.unhide()
        subcategoriesRect[currentSubcategory]?.unhide()
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
            colorComponents.add(this)
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