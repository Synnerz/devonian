package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.world.effect.MobEffectUtil
import net.minecraft.world.effect.MobEffects
import kotlin.math.pow

object ItemAnimations : Feature(
    "itemAnimations",
    category = Categories.VANILLA_TWEAKS,
    subcategory = "Animation",
) {
    private val SETTING_RESET = addButton(
        ::resetSettings,
        description = "only resets sliders",
        displayName = "Reset Values",
    )
    private val SETTING_X_OFFSET = addDecimalSlider(
        "xOffset",
        0.0,
        -1.0, 1.0,
        "",
        "X Offset",
    )
    private val SETTING_Y_OFFSET = addDecimalSlider(
        "yOffset",
        0.0,
        -1.0, 1.0,
        "",
        "Y Offset",
    )
    private val SETTING_Z_OFFSET = addDecimalSlider(
        "zOffset",
        0.0,
        -1.0, 1.0,
        "",
        "Z Offset",
    )
    private val SETTING_YAW_OFFSET = addSlider(
        "yawOffset",
        0.0,
        -180.0, 180.0,
        "",
        "Yaw Offset",
    )
    private val SETTING_PITCH_OFFSET = addSlider(
        "pitchOffset",
        0.0,
        -180.0, 180.0,
        "",
        "Pitch Offset",
    )
    private val SETTING_ROLL_OFFSET = addSlider(
        "rollOffset",
        0.0,
        -180.0, 180.0,
        "",
        "Roll Offset",
    )
    private val SETTING_SCALE = addDecimalSlider(
        "scale",
        0.0,
        -4.0, 4.0,
        "",
        "Item Size",
    )
    private val SETTING_CHANGE_HAND = addSwitch(
        "changeHand",
        true,
        "Should the above transformations affect an empty hand.",
        "Change Empty Hand",
    )
    private val SETTING_CHANGE_HOLDING_MAP = addSwitch(
        "changeMap",
        true,
        "Should the above transformations affect holding a map.",
        "Change Holding Map",
    )
    private val SETTING_SWING_SPEED = addDecimalSlider(
        "swingSpeed",
        0.0,
        -4.0, 4.0,
        "",
        "Swing Speed",
    )
    private val SETTING_IGNORE_EFFECTS = addSwitch(
        "ignoreEffects",
        false,
        "haste and mining fatigue (and conduit power)",
        "Ignore Effects",
    )
    private val SETTING_NO_REEQUIP_RESET = addSwitch(
        "noReequipReset",
        false,
        "removes item bob when swapping items",
        "No Reequip Reset",
    )
    private val SETTING_IN_PLACE_SWING = addSwitch(
        "inplaceSwing",
        false,
        "swing animation only rotates item",
        "In Place Swing Animation",
    )
    private val SETTING_NO_SWING = addSwitch(
        "noSwing",
        false,
        "removes swing animation entirely",
        "Disable Swing Animation",
    )
    private val SETTING_NO_SWING_TERM = addSwitch(
        "noSwingTerm",
        false,
        "removes swing animation from term",
        "Disable Terminator Swing",
    )
    private val SETTING_NO_HAND_SWAY = addSwitch(
        "noHandSway",
        false,
        "removes sway on item from turning",
        "No Hand Sway",
    )

    private fun resetSettings() {
        SETTING_X_OFFSET.set(0.0)
        SETTING_Y_OFFSET.set(0.0)
        SETTING_Z_OFFSET.set(0.0)
        SETTING_YAW_OFFSET.set(0.0)
        SETTING_PITCH_OFFSET.set(0.0)
        SETTING_ROLL_OFFSET.set(0.0)
        SETTING_SCALE.set(0.0)
        SETTING_SWING_SPEED.set(0.0)
    }

    fun disableReequip() = isEnabled() && SETTING_NO_REEQUIP_RESET.get()
    fun disableSwingTranslation(): Boolean {
        if (!isEnabled()) return false
        return SETTING_IN_PLACE_SWING.get()
    }
    fun disableSwingRotation(): Boolean {
        if (!isEnabled()) return false
        if (SETTING_NO_SWING.get()) return true
        if (!SETTING_NO_SWING_TERM.get()) return false

        val held = minecraft.player?.mainHandItem ?: return false
        if (held.isEmpty) return false
        return ItemUtils.skyblockId(held) == "TERMINATOR"
    }
    fun disableSwingBob(): Boolean {
        return disableSwingTranslation() || disableSwingRotation()
    }
    fun disableHandSway(): Boolean {
        return isEnabled() && SETTING_NO_HAND_SWAY.get()
    }
    fun affectHand(): Boolean = SETTING_CHANGE_HAND.get()
    fun affectMap(): Boolean = SETTING_CHANGE_HOLDING_MAP.get()

    private fun getItemScale() = 2.0.pow(SETTING_SCALE.get())
    private fun getSwingSpeed() = 2.0.pow(SETTING_SWING_SPEED.get())

    fun applyTransformations(pose: PoseStack) {
        if (!isEnabled()) return

        pose.rotate(Axis.XP.rotationDegrees(SETTING_PITCH_OFFSET.get().toFloat()))
        pose.rotate(Axis.YP.rotationDegrees(SETTING_YAW_OFFSET.get().toFloat()))
        pose.rotate(Axis.ZP.rotationDegrees(SETTING_ROLL_OFFSET.get().toFloat()))

        val xo = SETTING_X_OFFSET.get()
        val yo = SETTING_Y_OFFSET.get()
        val zo = SETTING_Z_OFFSET.get()
        if (xo != 0.0 || yo != 0.0 || zo != 0.0) pose.translate(
            xo.toFloat(),
            yo.toFloat(),
            zo.toFloat(),
        )
    }

    fun applyScale(pose: PoseStack) {
        if (!isEnabled()) return
        val scale = getItemScale().toFloat()
        if (scale != 1f) pose.scale(scale, scale, scale)
    }

    private fun getCurrentSwingDuration(): Int {
        if (SETTING_IGNORE_EFFECTS.get()) return 6
        val player = minecraft.player ?: return 6
        return if (MobEffectUtil.hasDigSpeed(player)) {
            6 - (1 + MobEffectUtil.getDigSpeedAmplification(player))
        } else {
            return 6 + (1 + (player.getEffect(MobEffects.MINING_FATIGUE)?.amplifier ?: -1)) * 2
        }
    }

    private var swinging = false
    private var swingTimeTick = 0
    private fun getSwingTime() = swingTimeTick * getSwingSpeed()
    private var attackAnim = 0f
    private var prevAttackAnim = 0f

    private fun getActualSwingAnimation(pt: Float): Float {
        var d = attackAnim - prevAttackAnim
        if (d < 0.0) d++
        return prevAttackAnim + d * pt
    }
    fun getSwingAnimation(pt: Float): Float {
        if (disableSwingRotation()) return 0f
        return getActualSwingAnimation(pt)
    }

    fun onSwing() {
        if (!isEnabled()) return
        if (swinging && swingTimeTick >= 0 && getSwingTime() < getCurrentSwingDuration() / 2) return
        swingTimeTick = -1
        swinging = true
    }

    fun onUpdateSwingTime() {
        if (!isEnabled()) return

        prevAttackAnim = attackAnim

        val total = getCurrentSwingDuration()

        if (swinging) {
            swingTimeTick++
            if (getSwingTime() >= total) {
                swingTimeTick = 0
                swinging = false
            }
        } else swingTimeTick = 0


        attackAnim = getSwingTime().toFloat() / total
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        swinging = false
        swingTimeTick = 0
    }
}