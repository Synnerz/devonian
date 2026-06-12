package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.talium.effects.UIEffect
import com.github.synnerz.talium.utils.Renderer
import java.awt.Color

class UILineEffect(
    var color: Color = Color.WHITE,
    var width: Double = 1.0,
    val left: Boolean = false,
) : UIEffect() {
    override fun preDraw(x2: Double, y2: Double) {
        if (component == null) return

        if (left) {
            Renderer.submitLine(
                (component!!.x - x2 + width).toFloat(),
                (component!!.y - y2).toFloat(),
                (component!!.x - x2 + width).toFloat(),
                (component!!.bounds.y2 - y2).toFloat(),
                width.toFloat(),
                color
            )
            return
        }

        Renderer.submitLine(
            (component!!.x - x2).toFloat(),
            (component!!.bounds.y2 - y2 - width).toFloat(),
            component!!.bounds.x2.toFloat(),
            (component!!.bounds.y2 - y2 - width).toFloat(),
            width.toFloat(),
            color
        )
    }
}