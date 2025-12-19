package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.api.events.GuiKeyUpEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import kotlin.math.sign

object ScrollableTooltip : Feature(
    "scrollableTooltip",
    "Allows you to use CTRL + Scroll to zoom in/out on a tooltip",
    subcategory = "Inventory"
) {
    private const val KEY_NAME = "scrollableTooltip"
    private val SETTING_ALLOW_HORIZONTAL = addSwitch(
        "allowHorizontal",
        true,
        "Allows you to use Shift + Scroll to move the tooltip sideways (left/right)",
        "Allow Horizontal"
    )
    private val SETTING_ALLOW_VERTICAL = addSwitch(
        "allowVertical",
        true,
        "Allows you to use Scroll to move the tooltip up/down",
        "Allow Vertical"
    )
    private var scaleScroll = 1.0
    private var xo = 0.0
    private var yo = 0.0
    private var holdingShift = false
    private var holdingCtrl = false

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
                    holdingCtrl -> scaleScroll *= 1 + (0.1 * sign(delta))
                    holdingShift -> {
                        if (SETTING_ALLOW_HORIZONTAL.get())
                            xo += 9 * sign(delta)
                    }
                    else -> {
                        if (SETTING_ALLOW_VERTICAL.get())
                            yo += 9 * sign(delta)
                    }
                }

                true
            }
        }

        on<GuiKeyDownEvent> { event ->
            if (event.key == GLFW.GLFW_KEY_LEFT_CONTROL) {
                holdingCtrl = true
                return@on
            }
            if (event.key != GLFW.GLFW_KEY_LEFT_SHIFT) return@on
            holdingShift = true
        }

        on<GuiKeyUpEvent> { event ->
            if (event.key == GLFW.GLFW_KEY_LEFT_CONTROL) {
                holdingCtrl = false
                return@on
            }
            if (event.key != GLFW.GLFW_KEY_LEFT_SHIFT) return@on
            holdingShift = false
        }
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
}