package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.CameraAccessor
import net.minecraft.client.Camera
import net.minecraft.world.entity.Pose

object ChangeCrouchHeight : Feature("changeCrouchHeight", "All changes are visual only") {
    private val SETTING_INSTANT_CROUCH = addSwitch(
        "instantCrouch",
        "",
        "Instant Crouch",
        true
    )
    private val SETTING_USE_189_HEIGHT = addSwitch(
        "legacyHeight",
        "",
        "Use 1.8.9 Crouch Height",
        true
    )

    fun getEyeHeight(): Float {
        val player = minecraft.player ?: return 0f
        return when (player.pose) {
            Pose.CROUCHING -> 1.54f
            else -> player.eyeHeight
        }
    }

    fun tick(camera: Camera): Boolean {
        if (camera !is CameraAccessor) return false

        if (camera.entity == null) return false
        if (camera.entity !== minecraft.player) return false

        val eye = if (SETTING_USE_189_HEIGHT.get()) getEyeHeight() else camera.entity.eyeHeight
        if (SETTING_INSTANT_CROUCH.get()) {
            camera.eyeHeightOld = eye
            camera.eyeHeight = eye
        } else {
            camera.eyeHeightOld = camera.eyeHeight
            camera.eyeHeight += (eye - camera.eyeHeight) * 0.5f
        }
        return true
    }
}