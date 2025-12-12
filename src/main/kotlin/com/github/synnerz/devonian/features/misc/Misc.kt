package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.features.Feature
import java.awt.Color

object RemoveFallingBlocks : Feature("removeFallingBlocks", subcategory = "Hiders")
object RemoveFireOverlay : Feature("removeFireOverlay", subcategory = "Hiders")
object NoHurtCamera : Feature("noHurtCamera", subcategory = "Hiders")
object RemoveLightning : Feature("removeLightning", subcategory = "Hiders")
object HideInventoryEffects : Feature("hideInventoryEffects", subcategory = "Hiders")
object HidePotionEffectOverlay : Feature("hidePotionEffectOverlay", subcategory = "Hiders")
object RemoveFrontView : Feature("removeFrontView", subcategory = "Tweaks")
object RemoveChatLimit : Feature("removeChatLimit", subcategory = "Tweaks")
object RemoveTabPing : Feature("removeTabPing", subcategory = "Hiders")
object DisableAttachedArrows : Feature("disableAttachedArrows", subcategory = "Hiders")
object DisableVignette : Feature("disableVignette", subcategory = "Hiders")
object DisableWaterOverlay : Feature("disableWaterOverlay", subcategory = "Hiders")
object DisableSuffocatingOverlay : Feature("disableSuffocatingOverlay", subcategory = "Hiders")
object DisableVanillaArmor : Feature("disableVanillaArmor", subcategory = "Hiders")
object DisableFog : Feature("disableFog", subcategory = "Hiders")
object ThirdPersonCrosshair : Feature("thirdPersonCrosshair", subcategory = "Tweaks")
object RemoveRecipeBook : Feature("removeRecipeBook", subcategory = "Hiders")
object RemoveContainerBackground : Feature("removeContainerBackground", subcategory = "Hiders")
object CustomContainerColor : Feature("customContainerColor", subcategory = "Tweaks") {
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
    subcategory = "Tweaks"
)
object DisableSwim : Feature("disableSwim", subcategory = "Tweaks", cheeto = true)
object CenteredCrosshair : Feature("centeredCrosshair", subcategory = "Tweaks")
object DisableEnderPearlCooldown : Feature("disableEnderPearlCooldown", subcategory = "Hiders")
object DisableHungerBar : Feature("disableHungerBar", subcategory = "Hiders")
object FixRedVignette : Feature("fixRedVignette", "fixes red vignettes (SA tp) from blocking clicks", subcategory = "Tweaks", cheeto = true)
object HideCraftingText : Feature("hideCraftingText", "in inventory above craftin menu", subcategory = "Hiders")