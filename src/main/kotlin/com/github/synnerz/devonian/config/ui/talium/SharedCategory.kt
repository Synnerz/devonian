package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.devonian.config.ConfigType
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.talium.components.*
import com.github.synnerz.talium.effects.OutlineEffect
import com.github.synnerz.talium.events.UIClickEvent
import com.github.synnerz.talium.events.UIFocusEvent
import com.github.synnerz.talium.events.UIKeyType

abstract class SharedCategory(displayName: String) {
    protected val NORMAL_TEXT_SCALE = 2.5f
    protected val DESCRIPTION_TEXT_SCALE = 1.8f

    protected val colorComponents = mutableListOf<UIColorPicker>()
    protected val switchComponents = mutableListOf<UISwitch>()

    protected val categoryTitle = UIText(4.0, 35.0, 100.0, 100.0, displayName, false).apply {
        setColor(ColorPalette.TEXT_COLOR)
        onResize { _, w ->
            textScale = 2f / w.scaleFactor
        }
    }
    protected val selectedCategoryOutline = UILineEffect(ColorPalette.OUTLINE2_COLOR, 1.5, true)

    protected data class UIPair(val container: UIElement, val content: UIElement)

    @Suppress("unchecked_cast")
    protected fun createComp(data: ConfigData<*>, parent: UIElement): UIPair {
        return createBase(if (data.parent == null) 0.0 else 1.5, parent).also { (container, content) ->
            content.addChild(createTitle(data.displayName))
            content.addChild(createDescription(data.description))
            content.addChild(
                when (data.type) {
                    ConfigType.SWITCH -> createSwitch(data as ConfigData.Switch)
                    ConfigType.SLIDER -> createSlider(data as ConfigData.Slider<Double>)
                    ConfigType.DECIMALSLIDER -> createDecimalSlider(data as ConfigData.DecimalSlider<Double>)
                    ConfigType.BUTTON -> createButton(data as ConfigData.Button)
                    ConfigType.TEXTINPUT -> createTextInput(data as ConfigData.TextInput)
                    ConfigType.SELECTION -> createSelection(data as ConfigData.Selection)
                    ConfigType.COLORPICKER -> createColorPicker(data as ConfigData.ColorPicker, container)
                    ConfigType.TIMESLIDER -> createTimeSlider(data as ConfigData.TimeSlider<Double>, container)
                }
            )
        }
    }

    protected fun createBase(offset: Double, parent: UIElement): UIPair {
        val container = UIBase(1.0, 0.0, 98.0, 15.0, parent)

        if (offset != 0.0) {
            UIText(
                0.0, 0.0,
                offset, 100.0,
                "|",
                centered = true,
                parent = container,
            ).apply {
                setColor(ColorPalette.LIGHT_TEXT_COLOR)
                onResize { _, w ->
                    textScale = 3f / w.scaleFactor
                }
            }
        }
        val outlineEffect = OutlineEffect(0.5, ColorPalette.OUTLINE_COLOR)

        return UIPair(
            container,
            UIRect(offset, 0.0, 100.0 - offset, 100.0, parent = container).apply {
                addEffects(outlineEffect)
                onMouseEnter {
                    outlineEffect.color = ColorPalette.OUTLINE2_COLOR
                }
                onMouseLeave {
                    outlineEffect.color = ColorPalette.OUTLINE_COLOR
                }
            }
        )
    }

    private fun createTitle(text: String, parent: UIElement? = null): UIText =
        UIText(0.0, 8.0, 100.0, 25.0, text, true, parent).apply {
            setColor(ColorPalette.TEXT_COLOR)
            onResize { _, w ->
                textScale = NORMAL_TEXT_SCALE / w.scaleFactor
            }
        }

    private fun createDescription(text: String, parent: UIElement? = null): UIWrappedText =
        UIWrappedText(2.0, 37.0, 75.0, 62.0, text, parent = parent).apply {
            setColor(ColorPalette.LIGHT_TEXT_COLOR)
            onResize { _, w ->
                textScale = DESCRIPTION_TEXT_SCALE / w.scaleFactor
            }
        }

    private fun createButton(
        configData: ConfigData.Button,
        parent: UIElement? = null
    ): UIRect = UIRect(80.0, 25.0, 15.0, 50.0, parent = parent).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(UIText(0.0, 0.0, 100.0, 100.0, configData.btnTitle, true).apply {
            setColor(ColorPalette.TEXT_COLOR)
            onResize { _, w ->
                textScale = NORMAL_TEXT_SCALE / w.scaleFactor
            }
        })
        onMouseRelease {
            if (!canTrigger()) return@onMouseRelease
            configData.onClick()
        }
    }

    protected fun createSwitch(configData: ConfigData.Switch, parent: UIElement? = null): UISwitch {
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

            switchComponents.add(this)
        }
    }

    protected fun createSlider(
        configData: ConfigData.Slider<Double>,
        parent: UIElement? = null
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

    protected fun createTimeSlider(
        configData: ConfigData.TimeSlider<Double>,
        parent: UIElement? = null
    ): UISlider =
        object : UISlider(80.0, 25.0, 15.0, 50.0, configData.get(), configData.min, configData.max, parent = parent) {
            override fun getDisplayValue(): String {
                return StringUtils.formatSeconds(value.toLong())
            }

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

    protected fun createDecimalSlider(
        configData: ConfigData.DecimalSlider<Double>,
        parent: UIElement? = null
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

    protected fun createTextInput(
        configData: ConfigData.TextInput,
        parent: UIElement? = null
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
        onCharType {
            configData.set(text)
        }
        configData.onChange {
            text = configData.get()
        }
    }

    protected fun createSelection(
        configData: ConfigData.Selection,
        parent: UIElement? = null
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

    protected fun createColorPicker(
        configData: ConfigData.ColorPicker,
        parent: UIElement? = null
    ): UIColorPicker = object : UIColorPicker(80.0, 25.0, 15.0, 50.0, configData.get(), parent) {
        init {
            colorComponents.add(this)
            floatingChild.onUnfocus {
                if (!arrowToggle || isMainClicked) return@onUnfocus
                hideDropdown()
            }
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

    open fun hide() {
        if (categoryTitle.parent != null)
            categoryTitle.parent!!.removeEffect(selectedCategoryOutline)
        categoryTitle.setColor(ColorPalette.TEXT_COLOR)
        switchComponents.forEach { it.initial = true }
    }

    open fun unhide() {
        if (categoryTitle.parent != null)
            categoryTitle.parent!!.addEffect(selectedCategoryOutline)
        categoryTitle.setColor(ColorPalette.LIGHT_TEXT_COLOR)
    }
}