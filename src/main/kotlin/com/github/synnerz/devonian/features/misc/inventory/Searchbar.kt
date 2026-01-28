package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.talium.components.UITextInput
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import java.awt.Color

object Searchbar : HudFeature(
    "searchbar",
    "Searches the typed characters in the current container's items name and/or lore (does not support multi-search or calculations yet)",
    subcategory = "Inventory",
) {
    private val SETTING_BACKGROUND_COLOR = addColorPicker(
        "bgcolor",
        Color(50, 50, 50, 255).rgb,
        "Background color of the searchbar",
        "Searchbar Background"
    )
    private val SETTING_NAME_MATCH_COLOR = addColorPicker(
        "nameMatchColor",
        Color(0, 255, 0, 255).rgb,
        "Background color of the items which matched their name",
        "Searchbar Name Match"
    )
    private val SETTING_LORE_MATCH_COLOR = addColorPicker(
        "loreMatchColor",
        Color(0, 255, 255, 255).rgb,
        "Background color of the items which matched their lore",
        "Searchbar Lore Match"
    )
    private val input = UITextInput(x, y, 15.0, 5.0).apply {
        setColor(SETTING_BACKGROUND_COLOR.getColor())
        SETTING_BACKGROUND_COLOR.onChange {
            setColor(SETTING_BACKGROUND_COLOR.getColor())
        }
        onKeyType { onKeyType() }
        onResize { _, _ -> onResize() }
    }
    private val highlightItems = mutableListOf<ItemMatch>()

    data class ItemMatch(val idx: Int, val inLore: Boolean = false)

    override fun onMouseDrag(dx: Double, dy: Double) {
        super.onMouseDrag(dx, dy)
        val window = minecraft.window
        input._x = (x / window.guiScaledWidth) * 100
        input._y = (y / window.guiScaledHeight) * 100
        input.setDirty()
    }

    override fun onKeyPress(keyCode: Int) {
        super.onKeyPress(keyCode)
        val window = minecraft.window
        input._x = (x / window.guiScaledWidth) * 100
        input._y = (y / window.guiScaledHeight) * 100
        input.setDirty()
    }

    override fun getBounds(): BoundingBox {
        val w = if (input.isDirty()) 144.0 else input.width
        val h = if (input.isDirty()) 25.0 else input.height
        return BoundingBox(x, y, w, h)
    }

    override fun drawImpl(ctx: GuiGraphics) {
        input.draw()
    }

    override fun sampleDraw(ctx: GuiGraphics, mx: Int, my: Int, selected: Boolean) {
        val pos = getBounds()

        ctx.fill(
            pos.x.toInt(),
            pos.y.toInt(),
            pos.x.toInt() + pos.w.toInt(),
            pos.y.toInt() + pos.h.toInt(),
            SETTING_BACKGROUND_COLOR.get()
        )
        super.sampleDraw(ctx, mx, my, selected)
    }

    override fun initialize() {
        on<PostRenderGuiEvent> {
            if (it.screen !is AbstractContainerScreen<*>) return@on

            draw(it.ctx)
        }

        on<ClientContainerCloseEvent> {
            highlightItems.clear()
        }

        on<ServerContainerCloseEvent> {
            highlightItems.clear()
        }

        on<GuiKeyDownEvent> { event ->
            if (event.screen !is AbstractContainerScreen<*>) return@on
            if (!input.focused) return@on

            input.handleKeyInput(event.key, event.scanCode)
            event.cancel()
        }

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            val data = highlightItems.find { it.idx == slot.index } ?: return@on
            val color = if (data.inLore) SETTING_LORE_MATCH_COLOR.get() else SETTING_NAME_MATCH_COLOR.get()

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color)
        }.prio = 30

        on<TickEvent> { event ->
            if (event.tick % 5 != 1) return@on
            val screen = minecraft.screen ?: return@on
            if (screen !is AbstractContainerScreen<*>) return@on

            onKeyType()
        }
    }

    private fun onResize() {
        val window = minecraft.window
        input._x = (x / window.guiScaledWidth) * 100
        input._y = (y / window.guiScaledHeight) * 100
        input.setDirty()
    }

    private fun onKeyType() {
        // TODO: add multi-search support
        val screen = minecraft.screen ?: return
        val container = screen as? AbstractContainerScreen<*> ?: return
        val items = container.menu.items
        val text = input.text

        highlightItems.clear()
        if (text.isEmpty()) return

        items.forEachIndexed { idx, item ->
            if (item.isEmpty) return@forEachIndexed
            val name = item.customName?.string ?: return@forEachIndexed
            val lore = ItemUtils.lore(item) ?: return@forEachIndexed

            if (name.lowercase().contains(text.lowercase())) {
                highlightItems.add(ItemMatch(idx))
                return@forEachIndexed
            }

            if (lore.any { it.lowercase().contains(text.lowercase()) }) {
                highlightItems.add(ItemMatch(idx, true))
                return@forEachIndexed
            }
        }
    }
}