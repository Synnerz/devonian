package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.AbstractContainerScreenAccessor
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.github.synnerz.devonian.utils.render.Render2D
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import java.awt.Color
import java.util.Optional

object EquipmentDisplay : Feature(
    "equipmentDisplay",
    "Displays the current equipments inside your inventory",
    subcategory = "Inventory",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Location.stateInSkyblock)
    }

    private val SETTING_ALIGNMENT = addSelection(
        "alignment",
        0,
        listOf("Left", "Right", "Far Right"),
        "The alignment of which the equipment will be placed at (from armor slots)",
        "Alignment"
    )
    private val SETTING_COMMAND_MODE = addSelection(
        "commandMode",
        0,
        listOf("stats", "equipment", "split"),
        "The command to be used whenever clicking the display (if \"split\" is selected half will run /stats the other half will run /eq)",
        "Command Mode"
    )
    private const val KEY_NAME = "equipment"
    private val backgroundSlotColor = Color(100, 100, 100, 150)
    private val borderSlotColor = Color(50, 50, 50, 150)
    private val equipmentSlots = setOf(10, 19, 28, 37)
    private var inGui = false
    private val equipment = MutableList<EquipmentItem>(4) { EquipmentItem.EMPTY }
    private val alignment get() = when (SETTING_ALIGNMENT.get()) {
        1 -> 19
        2 -> (27 * 2) - 2
        else -> -25
    }

    data class EquipmentItem(
        val texture: String? = null,
        val slot: Int = -1,
        val lore: List<String>? = null,
    ) {
        val itemStack by lazy {
            if (texture == null)
                ItemStack.EMPTY
            else
                ItemUtils.fakeSkull(texture).apply {
                    if (lore == null) return@apply
                    set(DataComponents.LORE, ItemLore(lore.map { Component.literal(it) }))
                }
        }

        companion object {
            val EMPTY = EquipmentItem()
        }
    }

    override fun initialize() {
        Config.set(KEY_NAME, JsonArray())
        Config.onAfterLoad {
            Config.get<List<JsonObject>>(KEY_NAME)?.forEachIndexed { idx, it ->
                if (idx > 3) return@forEachIndexed
                val obj = it.asJsonObject
                val textureObj = obj.get("texture") ?: return@forEachIndexed
                if (textureObj.isJsonNull) return@forEachIndexed
                val texture = textureObj.asString
                val loreObj = obj.get("lore") ?: return@forEachIndexed
                if (loreObj.isJsonNull) return@forEachIndexed
                val lore = loreObj.asJsonArray.map { it.asString }
                equipment.add(idx, EquipmentItem(texture, -1, lore))
            }
        }
        Config.onPreSave {
            val array = JsonArray()

            equipment.forEachIndexed { idx, it ->
                if (idx > 3) return@forEachIndexed
                val obj = JsonObject()
                val arr2 = JsonArray()
                it.lore?.forEach { l -> arr2.add(l) }
                obj.addProperty("texture", it.texture)
                obj.add("lore", arr2)
                array.add(obj)
            }

            Config.set(KEY_NAME, array)
        }

        on<ServerContainerOpenEvent> { event ->
            inGui = event.titleStr == "Stats \u0026 Equipment" || event.titleStr.endsWith(") Loadouts")
        }

        on<ServerContainerCloseEvent> { inGui = false }
        on<ClientContainerCloseEvent> { inGui = false }

        on<ServerContainerSetSlotEvent> { event ->
            if (!inGui) return@on
            val slot = event.slot
            if (slot !in equipmentSlots) return@on
            val itemStack = event.itemStack
            if (itemStack.isEmpty) {
                Scheduler.scheduleTask {
                    val idx = equipmentSlots.reversed().indexOf(slot)
                    if (idx == -1) return@scheduleTask

                    equipment.removeAt(idx)
                    equipment.add(idx, EquipmentItem(null, slot, null))
                }
                return@on
            }

            val texture = ItemUtils.texture(itemStack) ?: return@on
            Scheduler.scheduleTask {
                val idx = equipmentSlots.reversed().indexOf(slot)
                if (idx == -1) return@scheduleTask

                equipment.removeAt(idx)
                equipment.add(idx, EquipmentItem(
                    texture,
                    slot,
                    buildList {
                        // TODO: maybe fix the obfuscated color code not working properly
                        itemStack.customName?.let { add(it.colorCodes()) }
                        ItemUtils.lore(itemStack, true)?.let { addAll(it) }
                    }
                ))
            }
        }

        on<RenderSlotEvent> { event ->
            if (event.screen !is InventoryScreen) return@on
            val slot = event.slot
            val idx = slot.containerSlot
            if (idx !in 36..39) return@on
            val jdx = idx - 36
            val equipment = equipment.getOrNull(jdx) ?: return@on
            val alig = alignment
            val x = slot.x + alig

            event.ctx.fill(x, slot.y, x + 16, slot.y + 16, backgroundSlotColor.rgb)
            Render2D.drawWireRect(event.ctx, x, slot.y, 16, 16, borderSlotColor)
            event.ctx.fakeItem(equipment.itemStack, x, slot.y)
        }

        on<GuiClickEvent> { event ->
            if (!event.state || equipment.isEmpty() || event.screen !is InventoryScreen) return@on
            val screenAcc = event.screen as? AbstractContainerScreenAccessor ?: return@on

            equipment.forEachIndexed { idx, data ->
                // people might want this to still trigger even if empty?
                // if (data.lore == null && data.texture == null) return@forEachIndexed

                val slot = (event.screen as? AbstractContainerScreen<*> ?: return@forEachIndexed)
                    .menu
                    .slots.find { it.containerSlot == 36 + idx } ?: return@on
                val x = screenAcc.leftPos + slot.x + alignment.toDouble()
                val y = screenAcc.topPos + slot.y.toDouble()
                if (event.mx !in x..x + 16 || event.my !in y..y + 16) return@forEachIndexed

                if (SETTING_COMMAND_MODE.get() == 2) {
                    ChatUtils.command(if (idx >= 2) "stats" else "eq")
                    return@on
                }

                ChatUtils.command(SETTING_COMMAND_MODE.getCurrent())
                return@on // "on" so it fully returns
            }
        }

        on<RenderGuiEvent> { event ->
            val screen = event.screen as? AbstractContainerScreen<*> ?: return@on
            if (equipment.isEmpty() || screen !is InventoryScreen) return@on
            val screenAcc = event.screen as? AbstractContainerScreenAccessor ?: return@on

            equipment.forEachIndexed { idx, data ->
                // the slot is empty
                if (data.lore == null && data.texture == null) return@forEachIndexed
                val slot = screen
                    .menu
                    .slots.find { it.containerSlot == 36 + idx } ?: return@on
                val x = screenAcc.leftPos + slot.x + alignment.toDouble()
                val y = screenAcc.topPos + slot.y.toDouble()
                val mx = minecraft.mouseHandler.getScaledXPos(minecraft.window)
                val my = minecraft.mouseHandler.getScaledYPos(minecraft.window)

                if (mx !in x..x + 16 || my !in y..y + 16) return@forEachIndexed

                event.ctx.setTooltipForNextFrame(
                    minecraft.font,
                    data.itemStack.get(DataComponents.LORE)!!.lines,
                    Optional.empty(),
                    mx.toInt(),
                    my.toInt()
                )
                return@on // "on" so it fully returns
            }
        }
    }
}