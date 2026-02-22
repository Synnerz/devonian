package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.KeyPressEvent
import com.github.synnerz.devonian.api.events.KeyReleaseEvent
import com.github.synnerz.devonian.api.events.MousePressEvent
import com.github.synnerz.devonian.api.events.MouseReleaseEvent
import com.github.synnerz.devonian.api.events.MouseScrollEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.math.MathUtils
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW
import kotlin.math.pow
import kotlin.math.withSign

object ZoomKeybind : Feature(
    "zoomKeybind",
    "Hold key to zoom and/or scroll to zoom further/less (change the keybind in minecraft controls)",
    subcategory = "Tweaks"
) {
    private val SETTING_SCROLL_STEPS = addSlider(
        "scrollSteps",
        1.0,
        1.0, 10.0,
        "The steps it will go up/down whenever scrolling",
        "Zoom Steps"
    )

    val keybind = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devonian.zoomkey",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
    )

    private const val MAX_STEPS = 100.0
    private var currentStep = MAX_STEPS
    @JvmField
    var cachedFactor = 1f

    private fun ease(x: Double): Double {
        return x.pow(3)
    }

    private fun update(step: Double) {
        currentStep = step.coerceIn(0.0, MAX_STEPS)
        cachedFactor = ease(
            MathUtils.rescale(
                currentStep,
                0.0, MAX_STEPS,
                0.1, 1.0,
            )
        ).toFloat()
    }

    override fun initialize() {
        on<MouseScrollEvent> { event ->
            if (!keybind.isDown) return@on

            update(currentStep - SETTING_SCROLL_STEPS.get().withSign(event.delta))

            event.cancel()
        }

        on<KeyPressEvent> { event ->
            if (!keybind.matches(event.underlying)) return@on

            update(MAX_STEPS * 0.75)
        }

        on<MousePressEvent> { event ->
            if (!keybind.matchesMouse(event.mcEvent)) return@on

            update(MAX_STEPS * 0.75)
        }

        on<KeyReleaseEvent> { event ->
            if (!keybind.matches(event.underlying)) return@on

            update(MAX_STEPS)
        }

        on<MouseReleaseEvent> { event ->
            if (!keybind.matchesMouse(event.mcEvent)) return@on

            update(MAX_STEPS)
        }
    }
}