package com.github.synnerz.devonian.hud.texthud

interface DataProvider {
    var x: Double
    var y: Double
    var scale: Float
    var anchor: TextHud.Anchor
    var align: TextHud.Align
    var shadow: Boolean
    var backdrop: TextHud.Backdrop
}