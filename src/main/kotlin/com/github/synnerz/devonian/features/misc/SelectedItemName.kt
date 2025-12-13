package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.client.gui.GuiGraphics
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object SelectedItemName : TextHudFeature(
    "selectedItemName",
    "Cancels the selected item name above hotbar and adds a custom one that is movable",
    subcategory = "Tweaks",
) {
    fun onRender(ctx: GuiGraphics, ci: CallbackInfo) {
        if (!isEnabled()) return

        ci.cancel()
        // TODO: if possible, make fade in animation, it is done simply by adjusting the "l" as alpha to color
        val item = minecraft.player!!.mainHandItem!!
        val name = item.customName?.colorCodes() ?: item.itemName.string
        setLine(name)
        draw(ctx)
    }

    override fun getEditText(): List<String> = listOf("&cDungeonbreaker")
}