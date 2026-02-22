package com.github.synnerz.devonian.hud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.HudManagerHider
import com.github.synnerz.devonian.features.HudManagerInstructions
import com.github.synnerz.devonian.features.HudManagerName
import com.github.synnerz.devonian.features.HudManagerRenderer
import com.github.synnerz.devonian.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.min

object HudManager : Screen(Component.literal("Devonian.HudManager")) {
    var selectedHud: HudFeature? = null
        private set
    val huds = mutableListOf<HudFeature>()
    var isEditing = false

    private var mouseDown = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    fun initialize() {
        DevonianCommand.command.subcommand("huds") { _, args ->
            Scheduler.scheduleTask(1) {
                Devonian.minecraft.setScreen(this)
            }
            return@subcommand 1
        }

        Config.onAfterLoad {
            huds.forEach { it.load() }
        }

        Config.onPreSave {
            huds.forEach { it.save() }
        }
    }

    override fun init() {
        isEditing = true
        huds.forEach { if (!it.fromConfig) it.setDefaultValues() }
    }

    override fun removed() {
        selectedHud = null
        isEditing = false
        mouseDown = false
    }

    private fun updateSelected() {
        if (mouseDown) return
        selectedHud = huds.filter { it.isVisibleEdit() && it.inBounds(lastMouseX, lastMouseY) }
            .minByOrNull { it.getBounds().let { min(it.w, it.h) } + (if (it.isEnabled()) 0 else 1000000) }
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        lastMouseX = mouseX
        lastMouseY = mouseY
        updateSelected()
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        mouseDown = true
        updateSelected()
        selectedHud?.onMouseClick(mouseButtonEvent.x, mouseButtonEvent.y, mouseButtonEvent.button())

        return false
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        mouseDown = false

        return false
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        if (mouseButtonEvent.button() != 0) return false

        updateSelected()
        selectedHud?.onMouseDrag(d, e)

        return false
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        updateSelected()
        selectedHud?.onMouseScroll(verticalAmount)

        return false
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (keyEvent.key == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(keyEvent)

        if (selectedHud != null) {
            selectedHud?.onKeyPress(keyEvent.key)
            // updateSelected()
        }

        return false
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        for (hud in huds) {
            if (hud.isEnabled()) continue
            if (hud.isInternal) continue
            if (hud.isVisibleEdit()) hud.sampleDraw(context, mouseX, mouseY, hud == selectedHud)
        }

        val window = minecraft?.window ?: return
        context.fill(0, 0, window.guiScaledWidth, window.guiScaledHeight, 0x80000000.toInt())
        super.render(context, mouseX, mouseY, deltaTicks)

        Render2D.drawString(
            context,
            "Hud Manager",
            10, 10
        )

        Render2D.drawLine(
            context,
            window.guiScaledWidth * 0.5f, window.guiScaledHeight * 0.5f - 5f,
            window.guiScaledWidth * 0.5f, window.guiScaledHeight * 0.5f + 5f,
            Color.GREEN,
            1f / window.guiScale,
        )
        Render2D.drawLine(
            context,
            window.guiScaledWidth * 0.5f - 5f, window.guiScaledHeight * 0.5f,
            window.guiScaledWidth * 0.5f + 5f, window.guiScaledHeight * 0.5f,
            Color.GREEN,
            1f / window.guiScale,
        )

        for (hud in huds) {
            if (!hud.isEnabled() && !hud.isInternal) continue
            if (hud.isVisibleEdit()) hud.sampleDraw(context, mouseX, mouseY, hud == selectedHud)
        }
    }

    fun addHud(hud: HudFeature) {
        huds.add(hud)
    }
}