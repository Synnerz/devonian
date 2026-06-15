package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.events.PrePacketSentEvent
import com.github.synnerz.devonian.api.events.RenderTickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import org.lwjgl.glfw.GLFW
import java.awt.Color

object RemoveFallingBlocks : Feature("removeFallingBlocks", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object RemoveFireOverlay : Feature("removeFireOverlay", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object NoHurtCamera : Feature("noHurtCamera", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object RemoveLightning : Feature("removeLightning", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object HideInventoryEffects : Feature("hideInventoryEffects", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object HidePotionEffectOverlay : Feature("hidePotionEffectOverlay", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object RemoveFrontView : Feature("removeFrontView", category = Categories.VANILLA_TWEAKS)
object RemoveChatLimit : Feature("removeChatLimit", category = Categories.VANILLA_TWEAKS, subcategory = "Chat") {
    val SETTING_MAX_MESSAGES = addSlider(
        "maxMessages",
        1000.0,
        100.0, 10000.0,
        "",
        "Max Chat Messages",
    )
}
object RemoveTabPing : Feature("removeTabPing", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object RemoveTabHead : Feature("removeTabUselessHeads", "Removes the gray heads that hypixel sets in tablist", Categories.VANILLA_TWEAKS, subcategory = "Hider")
object DisableAttachedArrows : Feature("disableAttachedArrows", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object DisableVignette : Feature("disableVignette", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object DisableWaterOverlay : Feature("disableWaterOverlay", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object DisableSuffocatingOverlay : Feature("disableSuffocatingOverlay", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object DisableVanillaArmor : Feature("disableVanillaArmor", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object DisableFog : Feature("disableFog", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object ThirdPersonCrosshair : Feature("thirdPersonCrosshair", category = Categories.VANILLA_TWEAKS)
object RemoveRecipeBook : Feature("removeRecipeBook", category = Categories.VANILLA_TWEAKS, subcategory = "Container")
object RemoveContainerBackground : Feature("removeContainerBackground", category = Categories.VANILLA_TWEAKS, subcategory = "Container")
object CustomContainerColor : Feature("customContainerColor", category = Categories.VANILLA_TWEAKS, subcategory = "Container") {
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
    Categories.VANILLA_TWEAKS,
    subcategory = "Chat"
)
object CenteredCrosshair : Feature("centeredCrosshair", category = Categories.VANILLA_TWEAKS)
object DisableEnderPearlCooldown : Feature("disableEnderPearlCooldown", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object DisableHungerBar : Feature("disableHungerBar", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object FixRedVignette : Feature("fixRedVignette", "fixes red vignettes (SA tp) from blocking clicks", category = Categories.VANILLA_TWEAKS, cheeto = true)
object HideCraftingText : Feature("hideCraftingText", "in inventory above craftin menu", category = Categories.VANILLA_TWEAKS, subcategory = "Container")
object DisableNametagBackground : Feature("disableNametagBackground", category = Categories.VANILLA_TWEAKS,)
object DisableTextShadow : Feature("disableTextShadow", category = Categories.VANILLA_TWEAKS,)
object FixObfuscatedText : Feature("fixObfuscatedText", "fixes shifting around text", category = Categories.VANILLA_TWEAKS,)
object CustomSidebarColor : Feature("customSideBarColor", "Sets the color for scoreboard", category = Categories.VANILLA_TWEAKS,) {
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
object NametagShadow : Feature("nametagShadow", "Enables shadows on name tags", category = Categories.VANILLA_TWEAKS,)
object RemoveGlowEffect : Feature("removeGlowEffect", "Removes the glowing effect of every entity", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object SidebarTextShadow : Feature("sidebarTextShadow", "Adds shadows to the text that is rendered in the sidebar", category = Categories.VANILLA_TWEAKS,)
object AutoSprint : Feature("autoSprint", "Automatically sets the sprint key to true whenever you are walking", category = Categories.VANILLA_TWEAKS,)
object SignEnterKey : Feature(
    "signEnterKey",
    "Whenever pressing enter inside specific signs it'll act as if you hit confirm button. Shift to bypass.",
    Categories.VANILLA_TWEAKS,
) {
    fun shouldEnter(comps: List<Component>): Boolean {
        if (!isEnabled()) return false
        return GLFW.glfwGetKey(Devonian.minecraft.window.handle(), GLFW.GLFW_KEY_LEFT_SHIFT) != GLFW.GLFW_PRESS
    }
}
object RemoveHypixelScoreboard : Feature("removeHypixelScoreboard", "www.hypixel.net", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object DisableGlassPaneHighlight : Feature("disableGlassPaneHighlight", category = Categories.VANILLA_TWEAKS, subcategory = "Hider")
object HideUselessBossBar : Feature(
    "hideUselessBossBar",
    "",
    subcategory = "Hiders",
)
object HideHotbar : Feature(
    "hideHotbar",
    "Stops your hotbar from rendering",
    Categories.VANILLA_TWEAKS,
    subcategory = "Hider"
)
object HideHearts : Feature(
    "hideHearts",
    "Does not render your hearts",
    Categories.VANILLA_TWEAKS,
    subcategory = "Hider"
)
object HideScoreboard : Feature(
    "hideScoreboard",
    "Does not render your scoreboard",
    Categories.VANILLA_TWEAKS,
    subcategory = "Hider"
)
object HideExperience : Feature(
    "hideExperience",
    "Does not render the experience bar and level",
    Categories.VANILLA_TWEAKS,
    subcategory = "Hider"
) {
    val SETTING_REMOVE_BAR = addSwitch(
        "removeBar",
        true,
        "Removes the experience bar",
        "HideExperienceBar"
    )
    val SETTING_REMOVE_LEVEL = addSwitch(
        "removeLevel",
        true,
        "Removes the experience level",
        "HideExperienceLevel"
    )
}
object ConfirmDisconnect : Feature(
    "confirmDisconnect",
    "Allows you to set a time threshold of when your disconnect click should register rather than instantly disconnecting",
    Categories.VANILLA_TWEAKS,
) {
    val SETTING_THRESHOLD = addSlider(
        "threshold",
        150.0,
        0.0, 1000.0,
        "The threshold time",
        "Threshold"
    )
}
object FixRidingCamera : Feature(
    "fixRidingCamera",
    "Fixes MC-259512 (camera lags when riding something). (only visual)",
    Categories.VANILLA_TWEAKS,
)
object PlayerScale : Feature(
    "playerScale",
    "Changes your own player model's scale",
    Categories.VANILLA_TWEAKS,
) {
    val SETTING_SCALE = addDecimalSlider(
        "scale",
        1.0,
        0.1, 10.0,
        "Scale",
        "Scale"
    )

    fun scale(): Float = if (!isEnabled()) -1f else SETTING_SCALE.get().toFloat()
}
object OwnNameTag : Feature(
    "ownNameTag",
    "Shows your own name tag on third person",
    Categories.VANILLA_TWEAKS,
)
object AutoCopyScreenshot : Feature(
    "autoCopyScreenshot",
    "Copies taken screenshots into your clipboard (does not work in MacOS)",
    Categories.VANILLA_TWEAKS,
)
object FixCrimsonIsleFog : Feature(
    "fixCrimsonIsleFog",
    "Fixes the \"fog\" of crimson isles by removing night vision client side",
    Categories.VANILLA_TWEAKS,
) {
    fun shouldFix(): Boolean = isEnabled() && Location.area == "crimson isle"
}
object FixBowPull : Feature(
    "fixBowPull",
    "Fixes the bow pull whenever switching from normal arrow bow to a short bow",
    Categories.VANILLA_TWEAKS
) {
    private val shortbowIds = setOf(
        "ARTISANAL_SHORTBOW",
        "MACHINE_GUN_BOW",
        "SCORPION_BOW",
        "ITEM_SPIRIT_BOW",
        "MOSQUITO_BOW",
        "TERMINATOR",
    )
    private var sbId: String? = null

    override fun initialize() {
        on<PrePacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is ServerboundSetCarriedItemPacket) return@on
            val player = minecraft.player ?: return@on

            sbId = ItemUtils.skyblockId(player.inventory.getItem(packet.slot))
        }

        on<RenderTickEvent> {
            val player = minecraft.player ?: return@on
            if (!shortbowIds.contains(sbId)) return@on

            player.stopUsingItem()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        sbId = null
    }
}