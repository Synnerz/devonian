package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.devonian.config.ConfigType
import com.github.synnerz.talium.components.*
import com.github.synnerz.talium.effects.OutlineEffect
import com.github.synnerz.talium.events.UIClickEvent
import com.github.synnerz.talium.events.UIFocusEvent
import com.github.synnerz.talium.events.UIKeyType

class SearchCategory(rightPanel: UIBase) {
    private val NORMAL_TEXT_SCALE = 2.5f
    private val DESCRIPTION_TEXT_SCALE = 2f
    private val components = mutableListOf<Pair<ConfigData<*>, UIRect>>()
    private val colorComponents = mutableListOf<UIColorPicker>()
    private var previousText = ""
    private var oldCategory: Category? = null
    private val searchPanel = UITextInput(23.75, 92.0, 52.5, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)

        onKeyType { event ->
            // Unhide
            if (previousText.isEmpty() && text.isNotEmpty()) {
                oldCategory = ConfigGui.selectedCategory
                oldCategory?.hide()
                ConfigGui.selectedCategory = null
                this@SearchCategory.unhide()
            }
            // Hide
            else if (previousText.isNotEmpty() && text.isEmpty()) {
                hideColorPickers()
                this@SearchCategory.hide()

                if (oldCategory == null && ConfigGui.selectedCategory == null) {
                    ConfigGui.selectedCategory = ConfigGui.categories.first()
                    ConfigGui.selectedCategory?.unhide()
                } else if (ConfigGui.selectedCategory == null)
                    oldCategory?.unhide()

                oldCategory = null
                previousText = ""
                return@onKeyType
            }
            if (previousText !== text)
                onSearch(text)
            previousText = text
        }

        onResize { _, w ->
            textScale = DESCRIPTION_TEXT_SCALE / w.scaleFactor
        }
    }
    private val searchIcon = UIRect(72.25, 92.0, 4.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "\uD83D\uDD0E", true).apply {
            onResize { _, w ->
                textScale = NORMAL_TEXT_SCALE / w.scaleFactor
            }
        })
    }
    private val categoryTitleBg = UIRect(0.0, 0.0, 100.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
    }
    private val categoryTitle = UIText(0.0, 0.0, 100.0, 100.0, "Searching...", true, parent = categoryTitleBg).apply {
        setColor(ColorPalette.TEXT_COLOR)
        onResize { _, w ->
            textScale = 2f / w.scaleFactor
        }
    }
    private val scrollableRect = UIScrollable(0.0, 9.0, 100.0, 81.0, parent = rightPanel)

    init {
        create()
        hide()
    }

    private fun onSearch(str: String) {
        val shownFeats = Config.features.filterTo(mutableSetOf()) { matchesConfig(it, str) }
        val shownSubConfigs = Config.features.associateWith { it.subconfigs.filterTo(mutableSetOf()) { matchesConfig(it, str) } }

        var idx = 0
        components.forEach { (data, comp) ->
            val show = if (data is ConfigData.FeatureSwitch) {
                shownFeats.contains(data) ||
                shownSubConfigs[data]!!.isNotEmpty()
            } else {
                shownFeats.contains(data.parent) ||
                shownSubConfigs[data.parent]!!.contains(data)
            }
            if (show) {
                val i = idx++
                val y = 1.0 + i * 17.0
                comp._y = y
                comp.markDirty()
                comp.unhide()
                comp.update()
            } else comp.hide()
        }

        scrollableRect.updateScrollY(0.0)
        scrollableRect.yOffset = 0.0
        categoryTitle.text = "Searching \"$str\""
    }

    private fun matchesConfig(config: ConfigData<*>, str: String): Boolean {
        val tags = str.split(' ').map { it.lowercase().replace(ConfigData.searchStripReg, "") }
        return tags.any { config.searchTags.contains(it) }
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

    private fun create() {
        Config.features.forEach {
            createComp(it)
            it.subconfigs.forEach {
                createComp(it)
            }
        }
    }

    @Suppress("unchecked_cast")
    private fun createComp(data: ConfigData<*>) {
        if (data.isHidden && !Devonian.isDev) return

        components.add(
            Pair(
                data,
                createBase(0.0, scrollableRect, if (data is ConfigData.FeatureSwitch) 0.0 else 5.0).apply {
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
                }.also { it.hide() }
            )
        )
    }

    fun hide() {
        scrollableRect.hide()
        categoryTitleBg.hide()
        categoryTitle.text = "Searching..."
        previousText = ""
        oldCategory = null
    }

    fun unhide() {
        scrollableRect.unhide()
        categoryTitleBg.unhide()
        categoryTitle.text = "Searching..."
        oldCategory = null
    }

    private fun createBase(y: Double, parent: UIBase, offset: Double): UIRect =
        UIRect(1.0 + offset, y, 98.0 - offset, 15.0, parent = parent).apply {
            addEffects(OutlineEffect(1.0, ColorPalette.OUTLINE_COLOR))
        }

    private fun createTitle(text: String, parent: UIRect? = null): UIText =
        UIText(0.0, 2.0, 100.0, 25.0, text, true, parent).apply {
            setColor(ColorPalette.TEXT_COLOR)
            onResize { _, w ->
                textScale = NORMAL_TEXT_SCALE / w.scaleFactor
            }
        }

    private fun createDescription(text: String, parent: UIRect? = null): UIWrappedText =
        UIWrappedText(2.0, 28.0, 75.0, 75.0, text, parent = parent).apply {
            setColor(ColorPalette.LIGHT_TEXT_COLOR)
            onResize { _, w ->
                textScale = DESCRIPTION_TEXT_SCALE / w.scaleFactor
            }
        }

    private fun createButton(
        configData: ConfigData.Button,
        parent: UIRect? = null
    ): UIRect = UIRect(80.0, 25.0, 15.0, 50.0, parent = parent).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(UIText(0.0, 0.0, 100.0, 100.0, configData.btnTitle, true).apply {
            setColor(ColorPalette.TEXT_COLOR)
            onResize { _, w ->
                textScale = DESCRIPTION_TEXT_SCALE / w.scaleFactor
            }
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
    ): UISlider =
        object : UISlider(80.0, 25.0, 15.0, 50.0, configData.get(), configData.min, configData.max, parent = parent) {
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
    ): UIDecimalSlider = object :
        UIDecimalSlider(80.0, 25.0, 15.0, 50.0, configData.get(), configData.min, configData.max, parent = parent) {
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
        init {
            onResize { _, w ->
                textScale = DESCRIPTION_TEXT_SCALE / w.scaleFactor
            }
        }

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
    ): UISelection =
        object : UISelection(80.0, 25.0, 15.0, 50.0, configData.get(), configData.options, parent = parent) {
            init {
                centerText.onResize { _, w ->
                    centerText.textScale = DESCRIPTION_TEXT_SCALE / w.scaleFactor
                }
            }

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