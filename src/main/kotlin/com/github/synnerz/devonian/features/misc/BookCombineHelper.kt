package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.api.events.ServerContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetSlotEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Items
import java.awt.Color
import kotlin.jvm.optionals.getOrNull

object BookCombineHelper : Feature(
    "bookCombineHelper",
    "Highlights books that are of the same tier in your inventory of the one that is currently " +
            "placed in the anvil",
    subcategory = "Inventory",
    searchTags = setOf("book", "combine")
) {
    private val SETTING_HIGHLIGHT_COLOR = addColorPicker(
        "color",
        Color.CYAN.rgb,
        "the highlight color",
        "Highlight Color"
    )
    private val SETTING_AVOID_COMBINE_WRONG = addSwitch(
        "avoidWrong",
        false,
        "Cancels the combining click if you are combining the wrong one " +
                "(right clicking will override this)",
        "Avoid Combining Wrong"
    )
    private val WRONG_CLICK_SOUND = SoundEvents.NOTE_BLOCK_BASS
    private var inAnvil = false
    private val books = mutableListOf<BookSlot?>()
    private val validBooks = mutableListOf<BookSlot>()
    private var leftBook: String? = null
    private var rightBook: String? = null

    data class BookSlot(val name: String, val tier: Int, val slot: Int)

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            inAnvil = event.titleStr == "Anvil"
        }

        on<ServerContainerCloseEvent> {
            reset()
        }
        on<ClientContainerCloseEvent> {
            reset()
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (!inAnvil) return@on
            val slot = event.slot
            val itemStack = event.itemStack
            if (slot == 13 && itemStack.item == Items.ENCHANTED_BOOK) return@on
            if (slot == 33) {
                if (itemStack.item != Items.ENCHANTED_BOOK) {
                    rightBook = null
                    return@on
                }

                val extraAttributes = ItemUtils.extraAttributes(itemStack) ?: return@on
                val enchants = extraAttributes.getCompound("enchantments").getOrNull() ?: return@on
                if (enchants.size() != 1) return@on
                enchants.forEach { name, tag ->
                    rightBook = "$name:${tag.asInt().getOrNull() ?: return@forEach}"
                }
                return@on
            }
            if (slot != 29) return@on
            if (itemStack.item != Items.ENCHANTED_BOOK) {
                leftBook = null
                return@on
            }

            // doing it the lazy way
            val extraAttributes = ItemUtils.extraAttributes(itemStack) ?: return@on
            val enchants = extraAttributes.getCompound("enchantments").getOrNull() ?: return@on
            if (enchants.size() != 1) return@on
            enchants.forEach { name, tag ->
                leftBook = "$name:${tag.asInt().getOrNull() ?: return@forEach}"
            }
        }

        on<TickEvent> {
            if (!inAnvil) return@on
            val player = minecraft.player ?: return@on
            checkSlots()
            val inventory = player.inventory

            books.clear()

            inventory.forEachIndexed { idx, itemStack ->
                val name = itemStack.customName?.string ?: return@forEachIndexed
                if (name != "Enchanted Book") return@forEachIndexed
                val extraAttributes = ItemUtils.extraAttributes(itemStack) ?: return@forEachIndexed

                extraAttributes.getCompound("enchantments").getOrNull()?.forEach { name, tag ->
                    val level = tag.asInt().getOrNull() ?: return@forEach

                    val bookSlot = BookSlot(name, level, idx)
                    books.add(bookSlot)
                }
            }
        }

        on<RenderSlotEvent> { event ->
            if (!inAnvil) return@on
            val slot = event.slot
            if (!event.isInventory() || !validBooks.any { it.slot == slot.containerSlot }) return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SETTING_HIGHLIGHT_COLOR.get())
        }

        on<GuiClickEvent> { event ->
            if (!event.state || event.mbtn != 0 || !inAnvil || leftBook == null) return@on
            if (leftBook == rightBook) return@on
            val slot = ScreenUtils.cursorSlot(event.screen) ?: return@on
            val player = minecraft.player ?: return@on
            if (slot.container == player.inventory || slot.containerSlot != 22) return@on

            event.cancel()
            minecraft.level?.playPlayerSound(
                WRONG_CLICK_SOUND.value(),
                SoundSource.MASTER,
                1f, 0.5f,
            )
        }.setEnabled(SETTING_AVOID_COMBINE_WRONG.state)
    }

    fun reset() {
        inAnvil = false
        books.clear()
        validBooks.clear()
        leftBook = null
        rightBook = null
    }

    fun checkSlots() {
        validBooks.clear()

        if (leftBook == null) return
        val list = books.filterNotNull().filter { "${it.name}:${it.tier}" == leftBook }
        if (list.isEmpty()) return

        validBooks.addAll(list)
    }
}