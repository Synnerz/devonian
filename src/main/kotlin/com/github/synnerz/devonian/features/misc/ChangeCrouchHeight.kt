package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.CameraAccessor
import net.minecraft.client.Camera
import net.minecraft.world.entity.Pose

object ChangeCrouchHeight : Feature(
    "changeCrouchHeight",
    "All changes are visual only, unless otherwise listed.",
    Categories.VANILLA_TWEAKS,
    displayName = "Change Crouch",
    searchTags = setOf("sneak"),
) {
    private val SETTING_INSTANT_CROUCH = addSwitch(
        "instantCrouch",
        true,
        "removes animation from sneaking",
        "Instant Crouch",
    )
    private val SETTING_USE_189_HEIGHT = addSwitch(
        "legacyHeight",
        true,
        "",
        "Use 1.8.9 Crouch Height",
    )

    fun getEyeHeight(): Float {
        val player = minecraft.player ?: return 0f
        return getEyeHeight(player.pose)
    }

    fun getEyeHeight(pose: Pose): Float {
        val player = minecraft.player ?: return 0f
        if (SETTING_USE_189_HEIGHT.get() && !Location.stateInLatestArea.value && pose == Pose.CROUCHING) return 1.54f
        return player.getDimensions(pose).eyeHeight
    }

    private var wasCrouching = false

    fun tick(camera: Camera): Boolean {
        if (!isEnabled()) return false
        if (camera !is CameraAccessor) return false

        if (camera.entity() !== minecraft.player) return false

        val eye = getEyeHeight()
        val isCrouching = camera.entity().pose == Pose.CROUCHING
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