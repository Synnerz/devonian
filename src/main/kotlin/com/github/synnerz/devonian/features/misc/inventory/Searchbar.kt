package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.talium.components.UITextInput
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.lwjgl.glfw.GLFW
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
        onCharType {
            onKeyType()
        }
        onResize { _, _ -> onResize() }
    }
    private val highlightItems = mutableListOf<MatchType>()

    enum class MatchType {
        NAME,
        LORE,
        NONE,
    }
    enum class InputType {
        FULL,
        AND,
        OR,
        GREATER_THAN,
        LESSER_THAN,
        NONE,
    }

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

    override fun drawImpl(ctx: GuiGraphicsExtractor) {
        input.draw()
    }

    override fun sampleDraw(ctx: GuiGraphicsExtractor, mx: Int, my: Int, selected: Boolean) {
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
            Scheduler.scheduleTask { highlightItems.clear() }
        }

        on<GuiKeyDownEvent> { event ->
            if (event.screen !is AbstractContainerScreen<*>) return@on
            if ((event.event.modifiers and 2) != 0 && event.key == GLFW.GLFW_KEY_F)
                input.focused = true
            if (!input.focused) return@on

            input.handleKeyInput(event.key, event.scanCode)
            event.cancel()
        }

        on<GuiCharTypeEvent> { event ->
            val screen = minecraft.gui.screen() ?: return@on
            if (screen !is AbstractContainerScreen<*>) return@on
            if (!input.focused) return@on

            input.handleCharType(event.codepoint, event.str, -1)
            event.cancel()
        }

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            val data = highlightItems.getOrNull(slot.index) ?: return@on
            if (data == MatchType.NONE) return@on
            val color = if (data == MatchType.LORE) SETTING_LORE_MATCH_COLOR.get() else SETTING_NAME_MATCH_COLOR.get()

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color)
        }.prio = 30

        on<TickEvent> { event ->
            if (event.tick % 5 != 1) return@on
            val screen = minecraft.gui.screen() ?: return@on
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
        val screen = minecraft.gui.screen() ?: return
        val container = screen as? AbstractContainerScreen<*> ?: return
        val items = container.menu.items
        val text = input.text
        // TODO: improve mutli search
        val ( matchInput, matchType ) = when {
            text.contains("||") -> text.split("||") to InputType.OR
            text.contains("&&") -> text.split("&&") to InputType.AND
            text.contains(">") -> listOf(text.split(">")[1].replace(",", "")) to InputType.GREATER_THAN
            text.contains("<") -> listOf(text.split("<")[1].replace(",", "")) to InputType.LESSER_THAN
            text.isNotEmpty() -> listOf(text) to InputType.FULL
            else -> listOf<String>() to InputType.NONE
        }
        if (matchType == InputType.NONE) {
            highlightItems.clear()
            return
        }

        val arr = items.map { item ->
            if (text.isEmpty()) return@map MatchType.NONE
            if (item.isEmpty) return@map MatchType.NONE
            val itemName = item.customName?.string
            val itemLore = ItemUtils.lore(item)

            if (matchType != InputType.FULL) {
                return@map when (matchType) {
                    InputType.OR -> {
                        if (matchInput.any { itemName?.contains(it.trim(), ignoreCase = true) == true })
                            MatchType.NAME
                        else if (matchInput.any { p -> itemLore?.any { it.trim().contains(p, ignoreCase = true) } == true })
                            MatchType.LORE
                        else
                            MatchType.NONE
                    }
                    InputType.AND -> {
                        if (matchInput.all { itemName?.contains(it.trim(), ignoreCase = true) == true })
                            MatchType.NAME
                        else if (matchInput.all { p -> itemLore?.any { it.contains(p.trim(), ignoreCase = true) } == true })
                            MatchType.LORE
                        else
                            MatchType.NONE
                    }
                    InputType.GREATER_THAN -> {
                        val sbId = ItemUtils.skyblockId(item)
                        val itemPrice = sbId?.let { SkyblockPrices.buyPrice(it) } ?: -1f
                        val inpt = matchInput.first()
                        val regex = "\\d+[kbm]+".toRegex()
                        val parsedInput =
                            if (inpt.lowercase().matches(regex))
                                StringUtils.parseShortenedNumber(inpt.uppercase())
                            else
                                inpt.toIntOrNull() ?: -1

                        if (itemPrice != -1f && parsedInput != -1 && itemPrice >= parsedInput)
                            MatchType.NAME
                        else
                            MatchType.NONE
                    }
                    InputType.LESSER_THAN -> {
                        val sbId = ItemUtils.skyblockId(item)
                        val itemPrice = sbId?.let { SkyblockPrices.buyPrice(it) } ?: -1f
                        val inpt = matchInput.first()
                        val regex = "\\d+[kbm]+".toRegex()
                        val parsedInput =
                            if (inpt.lowercase().matches(regex))
                                StringUtils.parseShortenedNumber(inpt.uppercase())
                            else
                                inpt.toIntOrNull() ?: -1

                        if (itemPrice != -1f && parsedInput != -1 && itemPrice <= parsedInput)
                            MatchType.NAME
                        else
                            MatchType.NONE
                    }
                }
            }

            if (item.customName?.string?.contains(text, ignoreCase = true) == true) MatchType.NAME
                else if (ItemUtils.lore(item)?.any { it.contains(text, ignoreCase = true) } == true) MatchType.LORE
                else MatchType.NONE
        }

        highlightItems.clear()
        highlightItems.addAll(arr)
    }
}