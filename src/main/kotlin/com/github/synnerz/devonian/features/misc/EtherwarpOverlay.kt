package com.github.synnerz.devonian.features.misc

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.mixin.accessor.LocalPlayerAccessor
import com.github.synnerz.devonian.utils.BlockTypes
import com.github.synnerz.devonian.utils.math.DDA
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import java.awt.Color
import kotlin.math.hypot

object EtherwarpOverlay : Feature(
    "etherwarpOverlay",
    "Renders a box at the location where the etherwarp is going to be at."
) {
    private val SETTING_ETHER_WIRE_COLOR = addColorPicker(
        "wireColor",
        "",
        "Ether Outline Color",
        Color(46, 221, 23, 160).rgb
    )
    private val SETTING_ETHER_FILL_COLOR = addColorPicker(
        "fillColor",
        "",
        "Ether Fill Color",
        Color(96, 222, 85, 96).rgb
    )
    private val SETTING_ETHER_FAIL_WIRE_COLOR = addColorPicker(
        "failWireColor",
        "",
        "Ether Fail Outline Color",
        Color(202, 34, 7, 160).rgb
    )
    private val SETTING_ETHER_FAIL_FILL_COLOR = addColorPicker(
        "failFillColor",
        "",
        "Ether Fail Fill Color",
        Color(186, 43, 30, 96).rgb
    )
    private val SETTING_ETHER_USING_CANCEL_INTERACT = addSwitch(
        "usingCI",
        "Enables the etherwarp overlay even when looking at an interactable block",
        "Ether Using CI",
        false
    )
    private val SETTING_USE_SMOOTH_POSITION = addSwitch(
        "smooth",
        "Uses your camera position/look rather than the servers position/look",
        "Ether Use Smooth Position",
        false
    )

    private val validWeapons = mutableListOf("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID", "ETHERWARP_CONDUIT")
    var failReason = ""

    override fun initialize() {
        on<RenderWorldEvent> { event ->
            val player = minecraft.player
            if (minecraft.level == null || player == null) return@on
            val world = minecraft.level!!

            failReason = ""

            val heldItem = player.getItemInHand(InteractionHand.MAIN_HAND)
            if (
                heldItem.item != Items.DIAMOND_SHOVEL &&
                heldItem.item != Items.DIAMOND_SWORD &&
                heldItem.item != Items.PLAYER_HEAD
            ) return@on

            val itemId = ItemUtils.skyblockId(heldItem) ?: return@on
            val requireSneak = heldItem.item == Items.DIAMOND_SHOVEL || heldItem.item == Items.DIAMOND_SWORD

            if (requireSneak && !player.isSteppingCarefully) return@on
            if (!validWeapons.contains(itemId)) return@on

            val extraAttributes = ItemUtils.extraAttributes(heldItem) ?: return@on
            if (requireSneak && !extraAttributes.contains("ethermerge")) return@on

            if (!SETTING_ETHER_USING_CANCEL_INTERACT.get()) {
                val target = minecraft.hitResult
                if (target != null && target.type == HitResult.Type.BLOCK) {
                    val blockTarget = target as BlockHitResult
                    if (BlockTypes.Interactable.contains(world.getBlockState(blockTarget.blockPos).block)) return@on
                }
            }

            val tunedTransmission = extraAttributes.get("tuned_transmission")
            val tunedInt = tunedTransmission?.asInt()
            val tuners = if (tunedInt == null || tunedInt.isEmpty) 0 else tunedInt.get()
            val dist = 57 + tuners

            val px: Double
            val py: Double
            val pz: Double
            val lookVec: Vec3
            if (SETTING_USE_SMOOTH_POSITION.get()) {
                val posVec = player.getPosition(event.ctx.tickCounter().getGameTimeDeltaPartialTick(false))
                val camVec = player.getEyePosition(event.ctx.tickCounter().getGameTimeDeltaPartialTick(false))
                px = posVec.x
                py = camVec.y
                pz = posVec.z
                lookVec = player.getViewVector(event.ctx.tickCounter().getGameTimeDeltaPartialTick(false))
            } else {
                val playerAccessor = player as LocalPlayerAccessor
                px = playerAccessor.lastXClient
                py = playerAccessor.lastYClient + if (player.isShiftKeyDown) 1.54f else 1.62f
                pz = playerAccessor.lastZClient
                lookVec = player.calculateViewVector(playerAccessor.lastPitchClient, playerAccessor.lastYawClient)
            }

            var hitResult = raycast(
                px, py, pz,
                lookVec.x * dist,
                lookVec.y * dist,
                lookVec.z * dist
            )

            if (hitResult == null) {
                failReason = "&4Can't TP: Too far!"
                val maxDist = hypot(256.0, 16.0 * minecraft.options.effectiveRenderDistance)
                hitResult = raycast(
                    px + lookVec.x * dist,
                    py + lookVec.y * dist,
                    pz + lookVec.z * dist,
                    px + lookVec.x * maxDist,
                    py + lookVec.y * maxDist,
                    pz + lookVec.z * maxDist,
                )
                if (hitResult == null) return@on
            } else {
                val bpFoot = hitResult.above(1)
                val bpHead = hitResult.above(2)

                val bsFoot = world.getBlockState(bpFoot)
                val bsHead = world.getBlockState(bpHead)
                if (
                    !BlockTypes.AirLike.contains(bsFoot.block) ||
                    !BlockTypes.AirLike.contains(bsHead.block)
                ) failReason = "&4Can't TP: No air above!"
            }

            val camera = event.ctx.camera()
            val cameraPos = camera.position

            val outlineShape = world.getBlockState(hitResult).getShape(
                EmptyBlockGetter.INSTANCE,
                hitResult,
                CollisionContext.of(camera.entity)
            )

            Context.Immediate?.renderBoxShape(
                outlineShape,
                hitResult.x - cameraPos.x,
                hitResult.y - cameraPos.y,
                hitResult.z - cameraPos.z,
                if (failReason.isEmpty()) SETTING_ETHER_WIRE_COLOR.getColor() else SETTING_ETHER_FAIL_WIRE_COLOR.getColor(),
                true
            )

            Context.Immediate?.renderFilledShape(
                outlineShape,
                hitResult.x - cameraPos.x,
                hitResult.y - cameraPos.y,
                hitResult.z - cameraPos.z,
                if (failReason.isEmpty()) SETTING_ETHER_FILL_COLOR.getColor() else SETTING_ETHER_FAIL_FILL_COLOR.getColor(),
                true
            )
        }
    }

    private fun raycast(
        x: Double, y: Double, z: Double,
        dx: Double, dy: Double, dz: Double
    ): BlockPos? {
        val w = minecraft.level ?: return null

        for (bp in DDA(x, y, z, dx, dy, dz)) {
            val bs = w.getBlockState(bp)
            if (!BlockTypes.AirLike.contains(bs.block)) return bp
        }

        return null
    }
}