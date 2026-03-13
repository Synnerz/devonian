package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.PickupItemInventoryEvent
import com.github.synnerz.devonian.api.events.QuickMoveItemEvent
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.api.events.ServerContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetContentEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetSlotEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.FixedIdentityMap
import com.github.synnerz.devonian.utils.render.Render2D
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import java.awt.Color

object FavoriteAbiphone : Feature(
    "favoriteAbiphone",
    "Enables adding favorite contacts in the abiphone.",
    subcategory = "Inventory",
) {
    private val SETTING_HIGHLIGHT_COLOR = addColorPicker(
        "highlightColor",
        Color(0, 255, 255).rgb,
        "Color to highlight favorited contacts (when all contacts are shown).",
        "Favorite Contact Color",
    )

    private const val CONFIG_SHOW_KEY = "favoriteAbiphoneShow"
    private var showAll = false

    private const val CONFIG_FAV_KEY = "favoriteAbiphoneContacts"
    private val favoriteContacts = mutableSetOf<String>()

    private var inAbiphone = false

    private fun setFakeItem() {
        val item = if (showAll) Items.EMERALD else Items.DIAMOND
        val stack = ItemStack(item, 1)

        val name = if (showAll) "§e§lClick§r§f to only show §afavorite contacts"
            else "§e§lClick§r§f to show §ball contacts"
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name))
        stack.set(
            DataComponents.LORE,
            ItemLore(
                listOf(
                    "§e§lShift Click§r§7 to toggle a contact as a favorite.",
                ).map { Component.literal(it) }
            )
        )

        minecraft.player?.containerMenu?.getSlot(4)?.set(stack)
    }

    private val nameCache = FixedIdentityMap<ItemStack, String>(128)

    override fun initialize() {
        Config.set(CONFIG_SHOW_KEY, true)
        Config.set(CONFIG_FAV_KEY, JsonArray())

        Config.onAfterLoad {
            Config.get<Boolean>(CONFIG_SHOW_KEY)?.let {
                showAll = it
            }

            Config.get<List<JsonPrimitive>>(CONFIG_FAV_KEY)?.let {
                favoriteContacts.clear()
                it.forEach { v ->
                    if (!v.isString) return@forEach
                    favoriteContacts.add(v.asString)
                }
            }
        }

        Config.onPreSave {
            Config.set(CONFIG_SHOW_KEY, showAll)

            val arr = JsonArray()
            favoriteContacts.forEach { arr.add(it) }
            Config.set(CONFIG_FAV_KEY, arr)
        }

        on<ServerContainerOpenEvent> { event ->
            inAbiphone = event.titleStr.startsWith("Abiphone")
        }

        on<ServerContainerCloseEvent> {
            inAbiphone = false
        }

        on<ClientContainerCloseEvent> {
            inAbiphone = false
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (!inAbiphone) return@on

            if (event.slot != 4) return@on
            Scheduler.scheduleAfterPacket(::setFakeItem)
        }

        on<ServerContainerSetContentEvent> {
            if (!inAbiphone) return@on

            Scheduler.scheduleAfterPacket(::setFakeItem)
        }

        on<RenderSlotEvent> { event ->
            if (!inAbiphone) return@on

            val slot = event.slot
            if (event.isInventory()) return@on
            if (slot.containerSlot % 9 !in 1 .. 7) return@on
            if (slot.containerSlot / 9 !in 1 .. 4) return@on

            val stack = slot.item
            if (stack.isEmpty) return@on

            val name = nameCache.getOrPut(stack) {
                stack.customName?.string ?: ""
            }
            if (name.isEmpty()) return@on

            if (name in favoriteContacts) {
                if (showAll) {
                    Render2D.drawRect(
                        event.ctx,
                        event.slot.x, event.slot.y,
                        16, 16,
                        SETTING_HIGHLIGHT_COLOR.getColor(),
                    )
                }
            } else if (!showAll) event.cancel()
        }

        on<PickupItemInventoryEvent> { event ->
            if (!inAbiphone) return@on

            val slot = event.slot
            if (slot.container === minecraft.player?.inventory) return@on
            if (slot.containerSlot != 4) return@on

            showAll = !showAll
            setFakeItem()
            event.cancel()
        }

        on<QuickMoveItemEvent> { event ->
            if (!inAbiphone) return@on

            val slot = event.slot
            if (slot.container === minecraft.player?.inventory) return@on
            if (slot.containerSlot % 9 !in 1 .. 7) return@on
            if (slot.containerSlot / 9 !in 1 .. 4) return@on

            val stack = slot.item
            if (stack.isEmpty) return@on

            val name = stack.customName?.string ?: return@on

            if (name in favoriteContacts) favoriteContacts.remove(name)
            else favoriteContacts.add(name)

            event.cancel()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        nameCache.clear()
        inAbiphone = false
    }
}