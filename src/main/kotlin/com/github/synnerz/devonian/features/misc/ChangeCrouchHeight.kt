package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.CameraAccessor
import com.github.synnerz.devonian.mixin.accessor.EntityAccessor
import net.minecraft.client.Camera
import net.minecraft.world.entity.Pose

object ChangeCrouchHeight : Feature("changeCrouchHeight", "All changes are visual only") {
    private val SETTING_INSTANT_CROUCH = addSwitch(
        "instantCrouch",
        true,
        "",
        "Instant Crouch",
    )
    private val SETTING_USE_189_HEIGHT = addSwitch(
        "legacyHeight",
        true,
        "",
        "Use 1.8.9 Crouch Height",
    )
    private val SETTING_CHANGE_ACTUAL_HEIGHT = addSwitch(
        "nonVisual",
        false,
        "non visual",
        "Change Actual Crouch Height",
        cheeto = true,
    )

    fun getEyeHeight(): Float {
        val player = minecraft.player ?: return 0f
        return getEyeHeight(player.pose)
    }

    fun getEyeHeight(pose: Pose): Float {
        val player = minecraft.player ?: return 0f
        return when (pose) {
            Pose.CROUCHING -> 1.54f
            // otherwise recursion
            else -> (player as EntityAccessor).eyeHeightField
        }
    }

    fun changeNonVisual() = SETTING_CHANGE_ACTUAL_HEIGHT.get()

    private var wasCrouching = false

    fun tick(camera: Camera): Boolean {
        if (camera !is CameraAccessor) return false

        if (camera.entity == null) return false
        if (camera.entity !== minecraft.player) return false

        val eye = if (SETTING_USE_189_HEIGHT.get()) getEyeHeight() else camera.entity.eyeHeight
        val isCrouching = camera.entity.pose == Pose.CROUCHING
        if (SETTING_INSTANT_CROUCH.get() && (isCrouching || wasCrouching)) {
            camera.eyeHeightOld = eye
            camera.eyeHeight = eye
        } else {
            camera.eyeHeightOld = camera.eyeHeight
            camera.eyeHeight += (eye - camera.eyeHeight) * 0.5f
        }
        wasCrouching = isCrouching
        return true
    }
}