package com.github.synnerz.devonian.hud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import kotlin.math.min

object HudManager : Screen(Component.literal("Devonian.HudManager")) {
    private var selectedHud: HudFeature? = null
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

        huds.forEach { it._hudInit() }

        JsonUtils.preSave {
            for (hud in huds)
                hud.save()
        }
    }

    override fun init() {
        isEditing = true
    }

    override fun onClose() {
        super.onClose()

        selectedHud = null
        isEditing = false
    }

    private fun updateSelected() {
        if (mouseDown) return
        selectedHud = huds.filter { it.inBounds(lastMouseX, lastMouseY) }
            .minByOrNull { it.getBounds().let { min(it.w, it.h) } }
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        lastMouseX = mouseX
        lastMouseY = mouseY
        updateSelected()
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        mouseDown = true
        updateSelected()
        selectedHud?.onMouseClick(mouseX, mouseY, button)

        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        mouseDown = false

        return false
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        updateSelected()
        selectedHud?.onMouseDrag(deltaX, deltaY)

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

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(keyCode, scanCode, modifiers)

        if (selectedHud != null) {
            selectedHud?.onKeyPress(keyCode)
            // updateSelected()
        }

        return false
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.render(context, mouseX, mouseY, deltaTicks)
        Render2D.drawString(
            context,
            "Hud Manager",
            10, 10
        )

        for (hud in huds) hud.sampleDraw(context, mouseX, mouseY, hud == selectedHud)
    }

    fun addHud(hud: HudFeature) {
        huds.add(hud)
    }
}