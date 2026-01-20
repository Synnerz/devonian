package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.Holder
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color

object ArmorDisplay : HudFeature(
    "armorDisplay",
    "Displays the current armor you are wearing in a hud",
    subcategory = "Inventory"
) {
    private val SETTING_DRAW_BORDER = addSwitch(
        "drawBorder",
        true,
        "Whether to draw border around the armor display slots",
        "Armor Display Border"
    )
    private val SETTING_DRAW_BACKGROUND = addSwitch(
        "drawBackground",
        true,
        "Whether to draw background around the armor display slots",
        "Armor Display Background"
    )
    private val SETTING_DRAW_BARRIER = addSwitch(
        "drawBarrier",
        false,
        "Whether to draw a barrier whenever the armor display slot is empty",
        "Armor Display Barrier"
    )
    private val backgroundSlotColor = Color(100, 100, 100, 150)
    private val borderSlotColor = Color(50, 50, 50, 150)
    private val barrierItem = ItemStack(Holder.direct(Items.BARRIER))
    private val helmet: ItemStack?
        get() {
            val itemStack = minecraft.player?.getItemBySlot(EquipmentSlot.HEAD)
            if (SETTING_DRAW_BARRIER.get() && (itemStack == ItemStack.EMPTY || itemStack == null)) return barrierItem
            return itemStack
        }
    private val chestplate: ItemStack?
        get() {
            val itemStack = minecraft.player?.getItemBySlot(EquipmentSlot.CHEST)
            if (SETTING_DRAW_BARRIER.get() && (itemStack == ItemStack.EMPTY || itemStack == null)) return barrierItem
            return itemStack
        }
    private val leggings: ItemStack?
        get() {
            val itemStack = minecraft.player?.getItemBySlot(EquipmentSlot.LEGS)
            if (SETTING_DRAW_BARRIER.get() && (itemStack == ItemStack.EMPTY || itemStack == null)) return barrierItem
            return itemStack
        }
    private val boots: ItemStack?
        get() {
            val itemStack = minecraft.player?.getItemBySlot(EquipmentSlot.FEET)
            if (SETTING_DRAW_BARRIER.get() && (itemStack == ItemStack.EMPTY || itemStack == null)) return barrierItem
            return itemStack
        }

    override fun getBounds(): BoundingBox {
        return BoundingBox(x, y, 18.0 * scale, 72.0 * scale)
    }

    override fun drawImpl(ctx: GuiGraphics) {
        ctx.pose().pushMatrix()
        ctx.pose().translate(x.toFloat(), y.toFloat())
        ctx.pose().scale(scale)

        drawBorder(0, 0, ctx)
        ctx.renderFakeItem(helmet ?: barrierItem, 0, 0)
        drawBorder(0, 18, ctx)
        ctx.renderFakeItem(chestplate ?: barrierItem, 0, 18)
        drawBorder(0, 18 * 2, ctx)
        ctx.renderFakeItem(leggings ?: barrierItem, 0, 18 * 2)
        drawBorder(0, 18 * 3, ctx)
        ctx.renderFakeItem(boots ?: barrierItem, 0, 18 * 3)

        ctx.pose().popMatrix()
    }

    override fun sampleDraw(ctx: GuiGraphics, mx: Int, my: Int, selected: Boolean) {
        ctx.pose().pushMatrix()
        ctx.pose().translate(x.toFloat(), y.toFloat())
        ctx.pose().scale(scale)

        drawBorder(0, 0, ctx)
        ctx.renderFakeItem(barrierItem, 0, 0)
        drawBorder(0, 18, ctx)
        ctx.renderFakeItem(barrierItem, 0, 18)
        drawBorder(0, 18 * 2, ctx)
        ctx.renderFakeItem(barrierItem, 0, 18 * 2)
        drawBorder(0, 18 * 3, ctx)
        ctx.renderFakeItem(barrierItem, 0, 18 * 3)

        ctx.pose().popMatrix()

        super.sampleDraw(ctx, mx, my, selected)
    }

    override fun initialize() {
        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    private fun drawBorder(dx: Int, dy: Int, ctx: GuiGraphics) {
        if (SETTING_DRAW_BACKGROUND.get())
            ctx.fill(dx, dy, dx + 16, dy + 16, backgroundSlotColor.rgb)
        if (SETTING_DRAW_BORDER.get())
            Render2D.drawWireRect(ctx, dx, dy, 16, 16, borderSlotColor)
    }
}