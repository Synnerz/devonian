package com.github.synnerz.devonian.hud.texthud

class SimpleTextHud(name: String) : TextHud(
    name,
    StaticProvider(
        0.0, 0.0, 1f,
        Anchor.NW,
        Align.Left,
        false,
        Backdrop.None
    )
)

class StaticProvider(
    override var x: Double,
    override var y: Double,
    override var scale: Float,
    override var anchor: TextHud.Anchor,
    override var align: TextHud.Align,
    override var shadow: Boolean,
    override var backdrop: TextHud.Backdrop
) : DataProvider