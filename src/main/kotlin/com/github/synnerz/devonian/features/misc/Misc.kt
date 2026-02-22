package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.Color

object RemoveFallingBlocks : Feature("removeFallingBlocks", subcategory = "Hiders")
object RemoveFireOverlay : Feature("removeFireOverlay", subcategory = "Hiders")
object NoHurtCamera : Feature("noHurtCamera", subcategory = "Hiders")
object RemoveLightning : Feature("removeLightning", subcategory = "Hiders")
object HideInventoryEffects : Feature("hideInventoryEffects", subcategory = "Hiders")
object HidePotionEffectOverlay : Feature("hidePotionEffectOverlay", subcategory = "Hiders")
object RemoveFrontView : Feature("removeFrontView", subcategory = "Tweaks")
object RemoveChatLimit : Feature("removeChatLimit", subcategory = "Chat") {
    val SETTING_MAX_MESSAGES = addSlider(
        "maxMessages",
        1000.0,
        100.0, 10000.0,
        "",
        "Max Chat Messages",
    )
}
object RemoveTabPing : Feature("removeTabPing", subcategory = "Hiders")
object RemoveTabHead : Feature("removeTabUselessHeads", "Removes the gray heads that hypixel sets in tablist", subcategory = "Hiders")
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
    subcategory = "Chat"
)
object DisableSwim : Feature("disableSwim", subcategory = "Tweaks", cheeto = true)
object CenteredCrosshair : Feature("centeredCrosshair", subcategory = "Tweaks")
object DisableEnderPearlCooldown : Feature("disableEnderPearlCooldown", subcategory = "Hiders")
object DisableHungerBar : Feature("disableHungerBar", subcategory = "Hiders")
object FixRedVignette : Feature("fixRedVignette", "fixes red vignettes (SA tp) from blocking clicks", subcategory = "Tweaks", cheeto = true)
object HideCraftingText : Feature("hideCraftingText", "in inventory above craftin menu", subcategory = "Hiders")
object DisableNametagBackground : Feature("disableNametagBackground", subcategory = "Tweaks")
object DisableTextShadow : Feature("disableTextShadow", subcategory = "Tweaks")
object FixObfuscatedText : Feature("fixObfuscatedText", "fixes shifting around text", subcategory = "Tweaks")
object CustomSidebarColor : Feature("customSideBarColor", "Sets the color for scoreboard", subcategory = "Tweaks") {
    val SETTING_TITLE_COLOR = addColorPicker(
        "titleColor",
        Color(0, 0, 0, 100).rgb,
        "The scoreboard title background color",
        "Sidebar Title Color"
    )
    val SETTING_BODY_COLOR = addColorPicker(
        "titleColor",
        Color(0, 0, 0, 100).rgb,
        "The scoreboard body background color",
        "Sidebar Body Color"
    )
}
object NametagShadow : Feature("nametagShadow", "Enables shadows on name tags", subcategory = "Tweaks")
object RemoveGlowEffect : Feature("removeGlowEffect", "Removes the glowing effect of every entity", subcategory = "Hiders")
object SidebarTextShadow : Feature("sidebarTextShadow", "Adds shadows to the text that is rendered in the sidebar", subcategory = "Tweaks")
object AutoSprint : Feature("autoSprint", "Automatically sets the sprint key to true whenever you are walking", subcategory = "Tweaks")
object SignEnterKey : Feature(
    "signEnterKey",
    "Whenever pressing enter inside specific signs it'll act as if you hit confirm button. Shift to bypass.",
    subcategory = "Tweaks"
) {
    fun shouldEnter(comps: List<Component>): Boolean {
        if (!isEnabled()) return false
        return GLFW.glfwGetKey(Devonian.minecraft.window.handle(), GLFW.GLFW_KEY_LEFT_SHIFT) != GLFW.GLFW_PRESS
    }
}
object RemoveHypixelScoreboard : Feature("removeHypixelScoreboard", "www.hypixel.net", subcategory = "Hiders")
object DisableGlassPaneHighlight : Feature("disableGlassPaneHighlight", subcategory = "Hiders")
object HideUselessBossBar : Feature(
    "hideUselessBossBar",
    "",
    subcategory = "Hiders",
)
object HideHotbar : Feature(
    "hideHotbar",
    "Stops your hotbar from rendering",
    subcategory = "Hiders"
)
object HideHearts : Feature(
    "hideHearts",
    "Does not render your hearts",
    subcategory = "Hiders"
)
object HideScoreboard : Feature(
    "hideScoreboard",
    "Does not render your scoreboard",
    subcategory = "Hiders"
)
object HideExperience : Feature(
    "hideExperience",
    "Does not render the experience bar and level",
    subcategory = "Hiders"
)
object ConfirmDisconnect : Feature(
    "confirmDisconnect",
    "Allows you to set a time threshold of when your disconnect click should register rather than instantly disconnecting",
    subcategory = "Tweaks"
) {
    val SETTING_THRESHOLD = addSlider(
        "threshold",
        150.0,
        0.0, 1000.0,
        "The threshold time",
        "ConfirmDisconnect Threshold"
    )
}