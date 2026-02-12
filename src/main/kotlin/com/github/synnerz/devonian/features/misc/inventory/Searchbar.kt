package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.talium.components.UITextInput
import kotlinx.atomicfu.atomic
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
        setColor(Color(SETTING_BACKGROUND_COLOR.get(), true))
        SETTING_BACKGROUND_COLOR.onChange {
            setColor(Color(it, true))
        }
        onCharType { onKeyType() }
        onResize { _, _ -> onResize() }
    }
    private val highlightItems = atomic(intArrayOf())

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
            highlightItems.value = intArrayOf()
        }

        on<ServerContainerCloseEvent> {
            highlightItems.value = intArrayOf()
        }

        on<GuiKeyDownEvent> { event ->
            if (event.screen !is AbstractContainerScreen<*>) return@on
            if (!input.focused) return@on

            input.handleKeyInput(event.key, event.scanCode)
            event.cancel()
        }

        on<GuiCharTypeEvent> { event ->
            val screen = minecraft.screen ?: return@on
            if (screen !is AbstractContainerScreen<*>) return@on
            if (!input.focused) return@on

            input.handleCharType(event.codepoint, event.str, event.event.modifiers)
            event.cancel()
        }

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            val data = highlightItems.value.getOrNull(slot.index) ?: return@on
            if (data == 0) return@on
            val color = if (data == 1) SETTING_LORE_MATCH_COLOR.get() else SETTING_NAME_MATCH_COLOR.get()

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

        val arr = items.map { item ->
            if (text.isEmpty()) return@map 0
            if (item.isEmpty) return@map 0

            if (item.customName?.string?.contains(text, ignoreCase = true) == true) 2
                else if (ItemUtils.lore(item)?.any { it.contains(text, ignoreCase = true) } == true) 1
                else 0
        }

        highlightItems.value = arr.toIntArray()
    }
}