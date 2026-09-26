package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.PostRenderHotbarSlotEvent
import com.github.synnerz.devonian.api.events.PostRenderSlotEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.PersistentJsonClass
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import kotlin.toUInt

object CustomizeItems : Feature(
    "customizeItems",
    "Customize your items :) /dv <renameitem|changeitem|tintitem>",
    Categories.MISC,
    subcategory = "General",
) {
    data class CustomItemData(
        val names: MutableMap<String, String>,
        val models: MutableMap<String, String>,
        val tints: MutableMap<String, Int>,
    )

    val changedItems = object : PersistentJsonClass<CustomItemData>(
        "devonian/customItems.json",
        CustomItemData::class.java
    ) {
        override fun onLoadDefault() {
            data = CustomItemData(mutableMapOf(), mutableMapOf(), mutableMapOf())
        }
    }

    val nameComponents = mutableMapOf<String, Component>()
    val itemModels = mutableMapOf<String, Identifier>()

    val ItemRenderStateUUIDKey = RenderStateDataKey.create<String> { "Devonian Customize Items" }

    override fun initialize() {
        changedItems.onAfterLoad {
            changedItems.data!!.names.forEach { (k, v) ->
                nameComponents[k] = ChatUtils.literal(v)
            }
            changedItems.data!!.models.forEach { (k, v) ->
                itemModels[k] = Identifier.bySeparator(v, ':')
            }
        }
        changedItems.load()

        DevonianCommand.command.subcommand("renameitem", true) { _, args ->
            val name = args.firstOrNull() as String?

            val player = minecraft.player ?: return@subcommand 0
            val heldItem = player.mainHandItem

            val id = ItemUtils.uuid(heldItem)
            if (id == null) {
                ChatUtils.sendMessage("&4Cannot find item uuid.")
                return@subcommand 0
            }

            if (name == null) {
                nameComponents.remove(id)
                changedItems.data!!.names.remove(id)
                ChatUtils.sendMessage("&aRemoved custom name for item.")
            } else {
                nameComponents[id] = ChatUtils.literal(name)
                changedItems.data!!.names[id] = name
                ChatUtils.sendMessage("&aSet custom name to: $name")
            }

            1
        }
            .greedyString("name")

        DevonianCommand.command.subcommand("changeitem", true) { _, args ->
            val itemId = args.firstOrNull() as String?

            val player = minecraft.player ?: return@subcommand 0
            val heldItem = player.mainHandItem

            val id = ItemUtils.uuid(heldItem)
            if (id == null) {
                ChatUtils.sendMessage("&4Cannot find item uuid.")
                return@subcommand 0
            }

            if (itemId == null) {
                itemModels.remove(id)
                changedItems.data!!.models.remove(id)
                ChatUtils.sendMessage("&aRemoved custom model for item.")
            } else {
                itemModels[id] = Identifier.bySeparator(itemId, ':')
                changedItems.data!!.models[id] = itemId
                ChatUtils.sendMessage("&aSet custom model to: $itemId")
            }

            1
        }
            .greedyString("id")
            .suggest("id", *BuiltInRegistries.ITEM.keySet().map { it.toString() }.toTypedArray())

        DevonianCommand.command.subcommand("tintitem", true) { _, args ->
            val color = (args.firstOrNull() as String?)?.toUInt(16)?.toInt()

            val player = minecraft.player ?: return@subcommand 0
            val heldItem = player.mainHandItem

            val id = ItemUtils.uuid(heldItem)
            if (id == null) {
                ChatUtils.sendMessage("&4Cannot find item uuid.")
                return@subcommand 0
            }

            if (color == null) {
                changedItems.data!!.tints.remove(id)
                ChatUtils.sendMessage("&aRemoved custom color for item.")
            } else {
                changedItems.data!!.tints[id] = color
                val hexStr = color.toUInt().toHexString(HexFormat.UpperCase).padStart(8, '0')
                ChatUtils.sendMessage("&aSet custom color to: #${hexStr}")
            }

            1
        }
            .string("hexARGB")

        on<PostRenderSlotEvent> { event ->
            val slot = event.slot
            val item = slot.item
            val uuid = ItemUtils.uuid(item) ?: return@on
            val color = changedItems.data!!.tints[uuid] ?: return@on
            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color)
        }.prio = 20

        on<PostRenderHotbarSlotEvent> { event ->
            val item = event.item
            val uuid = ItemUtils.uuid(item) ?: return@on
            val color = changedItems.data!!.tints[uuid] ?: return@on
            event.ctx.fill(event.x, event.y, event.x + 16, event.y + 16, color)
        }.prio = 20
    }
}