package com.github.synnerz.devonian.hud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.HudManagerGrid
import com.github.synnerz.devonian.utils.render.Render2D
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.sdl.SDLKeycode
import java.awt.Color
import kotlin.math.min

object HudManager : Screen(Component.literal("Devonian.HudManager")) {
    private val selectedList = mutableListOf<HudPosition>()
    var selectedHud: HudFeature? = null
        private set
    val huds = mutableListOf<HudFeature>()
    var isEditing = false

    private var mouseDown = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    val gridSize: Double
        get() = 5.0 * HudManagerGrid.scale
    var cumDragX = 0.0
    var cumDragY = 0.0
    var startDragX = 0.0
    var startDragY = 0.0

    data class HudPosition(
        val hud: HudFeature,
        var cumDragX: Double = 0.0,
        var cumDragY: Double = 0.0,
        var startDragX: Double = 0.0,
        var startDragY: Double = 0.0,
    )

    fun initialize() {
        DevonianCommand.command.subcommand("huds") { _, args ->
            Scheduler.scheduleTask(1) {
                Devonian.minecraft.setScreenAndShow(this)
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
        huds.forEach {
            if (!it.fromConfig) it.setDefaultValues()
            it.fromConfig = true
        }
    }

    override fun removed() {
        selectedList.clear()
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
        // updateSelected()
        if (
            InputConstants.isKeyDown(SDLKeycode.SDLK_LCTRL) &&
            mouseButtonEvent.buttonInfo().button() == 0
        ) {
            val visibleHuds = huds.filter { it.isVisibleEdit() && it.inBounds(lastMouseX, lastMouseY) }
            selectedList.removeIf { visibleHuds.contains(it.hud) }
            selectedList.addAll(visibleHuds.map { HudPosition(
                it,
                0.0,
                0.0,
                it.x,
                it.y,
            ).also { it.hud.onMouseClick(mouseButtonEvent.x, mouseButtonEvent.y, mouseButtonEvent.button()) } })
        } else {
            selectedList.clear()
            selectedHud?.onMouseClick(mouseButtonEvent.x, mouseButtonEvent.y, mouseButtonEvent.button())
        }
        cumDragX = 0.0
        cumDragY = 0.0
        startDragX = selectedHud?.x ?: 0.0
        startDragY = selectedHud?.y ?: 0.0

        return false
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        mouseDown = false
        cumDragX = 0.0
        cumDragY = 0.0
        startDragX = 0.0
        startDragY = 0.0

        return false
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        if (mouseButtonEvent.button() != 0) return false

        // updateSelected()
        cumDragX += d
        cumDragY += e
        if (selectedList.isNotEmpty())
            selectedList.forEach {
                it.cumDragX += d
                it.cumDragY += e
                it.hud.onMouseDrag(d, e)
            }
        else
            selectedHud?.onMouseDrag(d, e)

        return false
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        // updateSelected()
        if (selectedList.isNotEmpty())
            selectedList.forEach {
                it.hud.onMouseScroll(verticalAmount)
            }
        else
            selectedHud?.onMouseScroll(verticalAmount)

        return false
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (keyEvent.key == SDLKeycode.SDLK_ESCAPE) return super.keyPressed(keyEvent)

        if (selectedList.isNotEmpty())
            selectedList.forEach {
                it.hud.onKeyPress(keyEvent.key)
            }
        else if (selectedHud != null) {
            selectedHud?.onKeyPress(keyEvent.key)
            // updateSelected()
        }

        return false
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        for (hud in huds) {
            if (hud.isEnabled()) continue
            if (hud.isInternal) continue
            if (hud.isVisibleEdit()) hud.sampleDraw(graphics, mouseX, mouseY, hud == selectedHud)
        }

        val window = minecraft?.window ?: return
        graphics.fill(0, 0, window.guiScaledWidth, window.guiScaledHeight, 0x80000000.toInt())
        super.extractRenderState(graphics, mouseX, mouseY, a)
        if (HudManagerGrid.isEnabled()) {
            var x = gridSize
            while (x < window.guiScaledWidth) {
                Render2D.drawLine(
                    graphics,
                    x.toFloat(), 0f,
                    x.toFloat(), window.guiScaledHeight.toFloat(),
                    Color(152, 191, 216, 50),
                    1f / window.guiScale,
                )
                x += gridSize
            }

            var y = gridSize
            while (y < window.guiScaledHeight) {
                Render2D.drawLine(
                    graphics,
                    0f, y.toFloat(),
                    window.guiScaledWidth.toFloat(), y.toFloat(),
                    Color(152, 191, 216, 50),
                    1f / window.guiScale,
                )
                y += gridSize
            }
        }

        Render2D.drawString(
            graphics,
            "Hud Manager",
            10, 10
        )

        Render2D.drawLine(
            graphics,
            window.guiScaledWidth * 0.5f, window.guiScaledHeight * 0.5f - 5f,
            window.guiScaledWidth * 0.5f, window.guiScaledHeight * 0.5f + 5f,
            Color.GREEN,
            1f / window.guiScale,
        )
        Render2D.drawLine(
            graphics,
            window.guiScaledWidth * 0.5f - 5f, window.guiScaledHeight * 0.5f,
            window.guiScaledWidth * 0.5f + 5f, window.guiScaledHeight * 0.5f,
            Color.GREEN,
            1f / window.guiScale,
        )

        for (hud in huds) {
            if (!hud.isEnabled() && !hud.isInternal) continue
            if (hud.isVisibleEdit()) hud.sampleDraw(graphics, mouseX, mouseY, hud == selectedHud)
        }
    }

    fun addHud(hud: HudFeature) {
        huds.add(hud)
    }
}