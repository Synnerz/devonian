package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.features.Feature
import java.awt.Color

object RemoveFallingBlocks : Feature("removeFallingBlocks")
object RemoveFireOverlay : Feature("removeFireOverlay")
object NoHurtCamera : Feature("noHurtCamera")
object RemoveLightning : Feature("removeLightning")
object HideInventoryEffects : Feature("hideInventoryEffects")
object HidePotionEffectOverlay : Feature("hidePotionEffectOverlay")
object RemoveFrontView : Feature("removeFrontView")
object RemoveChatLimit : Feature("removeChatLimit")
object RemoveTabPing : Feature("removeTabPing")
object DisableAttachedArrows : Feature("disableAttachedArrows")
object DisableVignette : Feature("disableVignette")
object DisableWaterOverlay : Feature("disableWaterOverlay")
object DisableSuffocatingOverlay : Feature("disableSuffocatingOverlay")
object DisableVanillaArmor : Feature("disableVanillaArmor")
object DisableFog : Feature("disableFog")
object ThirdPersonCrosshair : Feature("thirdPersonCrosshair")
object RemoveRecipeBook : Feature("removeRecipeBook")
object RemoveContainerBackground : Feature("removeContainerBackground")
object CustomContainerColor : Feature("customContainerColor") {
    val SETTING_CONTAINER_COLOR = addColorPicker(
        "containerColor",
        Color.WHITE.rgb,
        "The color which the container will use",
        "Container Color",
    )
}
object DisableChatAutoScroll : Feature(
    "disableChatAutoScroll",
    "Disables the auto scrolling to the latest message whenever the chat gui is focused",
    "Misc"
)
object DisableSwim : Feature("disableSwim", cheeto = true)
object CenteredCrosshair : Feature("centeredCrosshair")
object DisableEnderPearlCooldown : Feature("disableEnderPearlCooldown")