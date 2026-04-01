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
    private const val KEY_NAME = "equipment"
    private val backgroundSlotColor = Color(100, 100, 100, 150)
    private val borderSlotColor = Color(50, 50, 50, 150)
    private val equipmentSlots = setOf(10, 19, 28, 37)
    private var inGui = false
    private val equipment = mutableListOf<EquipmentItem>()
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
    }

    override fun initialize() {
        Config.set(KEY_NAME, JsonArray())
        Config.onAfterLoad {
            Config.get<List<JsonObject>>(KEY_NAME)?.forEach {
                val obj = it.asJsonObject
                val texture = obj.get("texture").asString
                val lore = obj.get("lore").asJsonArray.map { it.asString }
                equipment.add(EquipmentItem(texture, -1, lore))
            }
        }
        Config.onPreSave {
            val array = JsonArray()

            equipment.forEach {
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
            inGui = event.titleStr == "Your Equipment and Stats"
        }

        on<ServerContainerCloseEvent> { inGui = false }
        on<ClientContainerCloseEvent> { inGui = false }

        on<ServerContainerSetSlotEvent> { event ->
            if (!inGui) return@on
            val slot = event.slot
            if (slot !in equipmentSlots) return@on
            val itemStack = event.itemStack
            if (itemStack.isEmpty || itemStack.item != Items.PLAYER_HEAD) {
                Scheduler.scheduleTask {
                    equipment.removeIf { it.slot == slot }
                    equipment.add(EquipmentItem(null, slot, null))
                }
                return@on
            }

            val texture = ItemUtils.texture(itemStack) ?: return@on
            Scheduler.scheduleTask {
                equipment.removeIf { it.slot == slot }
                equipment.add(EquipmentItem(
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

            event.ctx.fill(slot.x + alig, slot.y, slot.x + alig + 16, slot.y + 16, backgroundSlotColor.rgb)
            Render2D.drawWireRect(event.ctx, slot.x + alig, slot.y, 16, 16, borderSlotColor)
            event.ctx.renderFakeItem(equipment.itemStack, slot.x + alig, slot.y)
        }

        on<GuiClickEvent> { event ->
            if (!event.state || equipment.isEmpty() || event.screen !is InventoryScreen) return@on
            val screenAcc = event.screen as? AbstractContainerScreenAccessor ?: return@on

            equipment.forEachIndexed { idx, data ->
                val slot = (event.screen as? AbstractContainerScreen<*> ?: return@forEachIndexed)
                    .menu
                    .slots.find { it.containerSlot == 39 - idx } ?: return@on
                val x = screenAcc.leftPos + slot.x + alignment.toDouble()
                val y = screenAcc.topPos + slot.y.toDouble()
                if (event.mx !in x..x + 16 || event.my !in y..y + 16) return@forEachIndexed

                ChatUtils.command("equipment")
                return@on // "on" so it fully returns
            }
        }

        on<RenderGuiEvent> { event ->
            val screen = event.screen as? AbstractContainerScreen<*> ?: return@on
            if (equipment.isEmpty() || screen !is InventoryScreen) return@on
            val screenAcc = event.screen as? AbstractContainerScreenAccessor ?: return@on

            equipment.forEachIndexed { idx, data ->
                val slot = screen
                    .menu
                    .slots.find { it.containerSlot == 39 - idx } ?: return@on
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