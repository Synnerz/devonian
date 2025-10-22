package com.github.synnerz.devonian.features.misc

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.ColorEnum
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.utils.JsonUtils
import net.minecraft.block.AbstractRailBlock
import net.minecraft.block.AbstractSkullBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.ButtonBlock
import net.minecraft.block.DyedCarpetBlock
import net.minecraft.block.SaplingBlock
import net.minecraft.block.ShapeContext
import net.minecraft.item.Items
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.world.EmptyBlockView
import net.minecraft.world.RaycastContext
import java.awt.Color

object EtherwarpOverlay : Feature("etherwarpOverlay") {
    private val validWeapons = mutableListOf("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID", "ETHERWARP_CONDUIT")
    private var color = ColorEnum.WHITE.color
    private var secondary = Color(color.red, color.green, color.blue, 80)

    override fun initialize() {
        JsonUtils.set("etherwarpOverlayColor", -1)

        DevonianCommand.command.subcommand("etherwarpoverlay") { _, args ->
            if (args.isEmpty()) return@subcommand 0
            color = ColorEnum.valueOf(args.first() as String).color
            secondary = Color(color.red, color.green, color.blue, 80)
            JsonUtils.set("etherwarpOverlayColor", color.rgb)
            ChatUtils.sendMessage("&aSuccessfully set etherwarp overlay color to &6${args.first()}", true)
            1
        }.string("color").suggest("color", *ColorEnum.entries.map { it.name }.toTypedArray())

        JsonUtils.afterLoad {
            color = Color(JsonUtils.get<Int>("etherwarpOverlayColor") ?: -1, true)
            secondary = Color(color.red, color.green, color.blue, 80)
        }

        on<RenderWorldEvent> { event ->
            val player = minecraft.player
            if (minecraft.world == null || player == null) return@on

            val heldItem = player.getStackInHand(Hand.MAIN_HAND)
            if (!(
                        heldItem.item == Items.DIAMOND_SHOVEL ||
                        heldItem.item == Items.DIAMOND_SWORD ||
                        heldItem.item == Items.PLAYER_HEAD
                    )) return@on

            val itemId = ItemUtils.skyblockId(heldItem) ?: return@on
            val requireSneak = heldItem.item == Items.DIAMOND_SHOVEL || heldItem.item == Items.DIAMOND_SWORD

            if (requireSneak && !player.isSneaking) return@on
            if (!validWeapons.any { it == itemId }) return@on

            val extraAttributes = ItemUtils.extraAttributes(heldItem) ?: return@on
            if (requireSneak && !extraAttributes.contains("ethermerge")) return@on

            val tunedTransmission = extraAttributes.get("tuned_transmission")
            val tunedInt = tunedTransmission?.asInt()
            val tuners = if (tunedInt == null || tunedInt.isEmpty) 0 else tunedInt.get()

            val ctx = event.ctx
            val hitResult = raycast(57 + tuners) ?: return@on
            val camera = minecraft.gameRenderer.camera ?: return@on
            val cam = camera.pos
            val blockPos = hitResult.blockPos
            val posFoot = blockPos.up()
            val posHead = blockPos.up(2)

            val blockFoot = minecraft.world!!.getBlockState(posFoot)
            val blockHead = minecraft.world!!.getBlockState(posHead)
            if (!blockFoot.isValid() || !blockHead.isValid()) return@on

            val blockState = minecraft.world!!.getBlockState(blockPos)
            if (blockState.isValid()) return@on

            val outlineShape = blockState.getOutlineShape(
                EmptyBlockView.INSTANCE,
                blockPos,
                ShapeContext.of(camera.focusedEntity)
            )

            Context.Immediate?.renderBoxShape(
                outlineShape,
                blockPos.x - cam.x,
                blockPos.y - cam.y,
                blockPos.z - cam.z,
                color,
                true
            )

            Context.Immediate?.renderFilledShape(
                outlineShape,
                blockPos.x - cam.x,
                blockPos.y - cam.y,
                blockPos.z - cam.z,
                secondary,
                true
            )
        }
    }

    private fun raycast(dist: Int): BlockHitResult? {
        val player = minecraft.player ?: return null
        val sum = if (player.isSneaking) 1.54 else 1.7
        val playerPos = player.pos.add(0.0, sum, 0.0)
        val entityPos = minecraft.cameraEntity?.getRotationVec(
                minecraft.renderTickCounter.getTickProgress(true)
            )!!
        val end = playerPos.add(entityPos.x * dist, entityPos.y * dist, entityPos.z * dist)

        val hitResult = minecraft.world!!.raycast(
            RaycastContext(
                playerPos,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.ANY,
                minecraft.cameraEntity!!
            )
        )
        if (hitResult.type !== HitResult.Type.BLOCK) return null

        return hitResult
    }

    private fun BlockState.isValid(): Boolean {
        return when (block) {
            // TODO: missing snow_layer, double_plant, piston_extension
            Blocks.AIR,
            Blocks.FIRE,
            Blocks.LEVER,
            Blocks.TORCH,
            Blocks.TRIPWIRE,
            Blocks.TRIPWIRE_HOOK,
            Blocks.CARROTS,
            Blocks.WHEAT,
            Blocks.POTATOES,
            Blocks.NETHER_WART,
            Blocks.PUMPKIN_STEM,
            Blocks.MELON_STEM,
            Blocks.REDSTONE_TORCH,
            Blocks.REDSTONE_WIRE,
            Blocks.FLOWER_POT,
            Blocks.DEAD_BUSH,
            Blocks.TALL_GRASS,
            Blocks.LADDER,
            Blocks.REPEATER,
            Blocks.COMPARATOR,
            Blocks.COBWEB,
            Blocks.WATER,
            Blocks.LILY_PAD,
            Blocks.LAVA,
            Blocks.VINE,
            Blocks.BROWN_MUSHROOM,
            Blocks.RED_MUSHROOM,
            Blocks.DANDELION,
            Blocks.POPPY,
            Blocks.SHORT_GRASS,
            Blocks.FERN -> true
            is SaplingBlock -> true
            is AbstractRailBlock -> true
            is ButtonBlock -> true
            is AbstractSkullBlock -> true
            is DyedCarpetBlock -> true
            else -> false
        }
    }
}