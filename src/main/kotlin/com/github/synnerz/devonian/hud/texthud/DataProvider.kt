package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.*

interface DataProvider {
    var x: Double
    var y: Double
    var scale: Float
    var anchor: Anchor
    var align: Align
    var shadow: Boolean
    var backdrop: Backdrop
}

class StaticProvider(
    override var x: Double,
    override var y: Double,
    override var scale: Float,
    override var anchor: Anchor,
    override var align: Align,
    override var shadow: Boolean,
    override var backdrop: Backdrop
) : DataProvider