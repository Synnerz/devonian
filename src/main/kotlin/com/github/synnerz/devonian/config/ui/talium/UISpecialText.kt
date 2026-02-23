package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.talium.components.UIElement
import com.github.synnerz.talium.components.UIText

class UISpecialText(
    _x: Double,
    _y: Double,
    _width: Double,
    _height: Double,
    text: String = "",
    centered: Boolean = false,
    parent: UIElement? = null
) : UIText(_x, _y, _width, _height, text, centered, parent) {
    var _onSelectHook: ((Boolean) -> Unit)? = null

    fun select(state: Boolean) {
        _onSelectHook?.invoke(state)
    }

    fun onSelect(cb: (Boolean) -> Unit) {
        _onSelectHook = cb
    }
}