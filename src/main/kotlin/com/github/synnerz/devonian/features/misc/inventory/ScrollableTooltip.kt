package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.api.events.GuiKeyUpEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import org.lwjgl.sdl.SDLKeycode
import kotlin.math.sign

object ScrollableTooltip : Feature(
    "scrollableTooltip",
    "Allows you to use move a tooltip.",
    Categories.VANILLA_TWEAKS,
    subcategory = "Tooltip",
) {
    private const val KEY_NAME = "scrollableTooltip"
    private val SETTING_ALLOW_HORIZONTAL = addSwitch(
        "allowHorizontal",
        true,
        "Allows you to use Shift + Scroll to move the tooltip sideways (left/right).",
        "Allow Horizontal",
    )
    private val SETTING_ALLOW_VERTICAL = addSwitch(
        "allowVertical",
        true,
        "Allows you to use Scroll to move the tooltip up/down.",
        "Allow Vertical",
    )
    private val SETTING_ALLOW_SCALE = addSwitch(
        "allowScale",
        true,
        "Allows you to use Control + Scroll to scale the tooltip.",
        "Allow Scaling",
    )
    val SETTING_LOCK_IN_PLACE = addSwitch(
        "lockInPlace",
        false,
        "Allows you to lock in place the tooltip to always be at the customized position regardless of the underlying tooltip shift.",
        "Tooltip Lock In Place",
    )
    val SETTING_DONT_DIVIDE_BY_SCALE = addSwitch(
        "dontDivideByScale",
        false,
        "Ignores the scaling factor whenever Lock In Place is enabled.",
        "Tooltip Ignore Scale",
    )
    private val SETTING_RESET_TOOLTIP = addSwitch(
        "resetTooltip",
        true,
        "Resets the x, y offset every time you hover over a new item.",
        "Tooltip Reset",
    )
    private val SETTING_RESET_ALL = addButton(::reset, displayName = "Reset")
    private var scaleScroll = 1.0
    private var xo = 0.0
    private var yo = 0.0
    private var holdingShift = false
    private var holdingCtrl = false
    private var holdingAlt = false
    private var lastEq: Slot? = null

    override fun initialize() {
        Config.set(KEY_NAME, JsonObject())

        Config.onAfterLoad {
            val obj = Config.get<Map<String, JsonPrimitive>>(KEY_NAME) ?: return@onAfterLoad
            for (data in obj) {
                val name = data.key
                val v = data.value

                if (name == "x") {
                    xo = v.asDouble
                    continue
                }
                if (name == "y") {
                    yo = v.asDouble
                    continue
                }
                if (name != "scale") continue

                scaleScroll = v.asDouble
            }
        }

        Config.onPreSave {
            val obj = JsonObject()

            obj.addProperty("x", xo)
            obj.addProperty("y", yo)
            obj.addProperty("scale", scaleScroll)

            Config.set(KEY_NAME, obj)
        }

        ScreenEvents.BEFORE_INIT.register { _, screen, x, y ->
            ScreenMouseEvents.afterMouseScroll(screen).register { _, _, _, _, delta, _ ->
                val itemStack = ScreenUtils.cursorStack(screen) ?: return@register true
                if (itemStack == ItemStack.EMPTY) return@register true

                when {
                    holdingCtrl -> {
                        if (SETTING_ALLOW_SCALE.get()) {
                            scaleScroll *= 1 + (0.1 * sign(delta))
                        }
                    }
                    holdingShift -> {
                        if (SETTING_ALLOW_HORIZONTAL.get()) {
                            xo += if (holdingAlt) 4 * sign(delta)
                            else 9 * sign(delta)
                        }
                    }
                    else -> {
                        if (SETTING_ALLOW_VERTICAL.get()) {
                            yo += if (holdingAlt) 4 * sign(delta)
                            else 9 * sign(delta)
                        }
                    }
                }

                true
            }
        }

        on<GuiKeyDownEvent> { event ->
            if (event.key == SDLKeycode.SDLK_LCTRL) {
                holdingCtrl = true
                return@on
            }
            if (event.key == SDLKeycode.SDLK_LALT) {
                holdingAlt = true
                return@on
            }

            if (event.key != SDLKeycode.SDLK_LSHIFT) return@on
            holdingShift = true
        }

        on<GuiKeyUpEvent> { event ->
            if (event.key == SDLKeycode.SDLK_LCTRL) {
                holdingCtrl = false
                return@on
            }
            if (event.key == SDLKeycode.SDLK_LALT) {
                holdingAlt = false
                return@on
            }

            if (event.key != SDLKeycode.SDLK_LSHIFT) return@on
            holdingShift = false
        }
    }

    private fun reset() {
        scaleScroll = 1.0
        xo = 0.0
        yo = 0.0
        lastEq = null
    }

    fun scale(): Double {
        if (!isEnabled()) return 1.0
        return scaleScroll.coerceAtLeast(0.0)
    }

    fun xoffset(): Double {
        if (!isEnabled()) return 0.0
        return xo
    }

    fun yoffset(): Double {
        if (!isEnabled()) return 0.0
        return yo
    }

    fun onRender(x: Int, y: Int, xoffset: Int, yoffset: Int) {
        val screen = minecraft.gui.screen() ?: return
        val eq = ScreenUtils.cursorSlot(screen) ?: return
        if (lastEq === eq) return

        if (SETTING_RESET_TOOLTIP.get()) {
            xo = 0.0
            yo = 0.0
        }

        lastEq = eq
    }
}