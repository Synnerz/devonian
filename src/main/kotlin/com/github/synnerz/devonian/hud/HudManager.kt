package com.github.synnerz.devonian.hud

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.devonian.features.HudManagerGrid
import com.github.synnerz.devonian.utils.render.Render2D
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.awt.Color
import kotlin.math.min

object HudManager : Screen(Component.literal("Devonian.HudManager")) {
    val selectedHuds = linkedMapOf<HudFeature, HudPosition?>()
    var hoveredHud: HudFeature? = null
    private var hoveredPos: HudPosition? = null
    var allShownHuds = emptyList<HudFeature>()
        private set
    val huds = mutableListOf<HudFeature>()
    var isEditing = false
        private set

    val showMoveHudFeatureButton = ConfigData.Switch(
        "showHudFeatureMoveButton",
        true,
        null,
        "Whether to show the move hud button under every text hud feature.",
    )

    private var mouseDown = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    val gridSize: Double
        get() = 5.0 * HudManagerGrid.scale

    data class HudPosition(
        val startDragX: Double,
        val startDragY: Double,
        var cumDragX: Double = 0.0,
        var cumDragY: Double = 0.0,
    )

    fun show(feat: HudFeature) {
        show(listOf(feat))
    }

    fun show(feats: List<HudFeature> = huds) {
        allShownHuds = (feats.toSet() + huds.filter { it.isInternal }).toList()
        Devonian.minecraft.setScreenAndShow(this)
    }

    fun initialize() {
        DevonianCommand.command.subcommand("huds") { _, _ ->
            Scheduler.scheduleTask(1) {
                show()
            }
            return@subcommand 1
        }

        Config.registerCategory(showMoveHudFeatureButton, Categories.GLOBAL, "Mod")

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
        selectedHuds.clear()
        hoveredHud = null
        hoveredPos = null
        allShownHuds = emptyList()
        isEditing = false
        mouseDown = false
    }

    private fun updateHovered() {
        if (mouseDown) return
        hoveredHud = allShownHuds
            .filter { it.isVisibleEdit() && it.inBounds(lastMouseX, lastMouseY) }
            .minByOrNull { it.getBounds().let { min(it.w, it.h) } + (if (it.isEnabled()) 0 else 1000000) }
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        lastMouseX = mouseX
        lastMouseY = mouseY
        updateHovered()
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        mouseDown = true
        // updateSelected()
        if (
            InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) &&
            mouseButtonEvent.buttonInfo().button() == 1
        ) {
            allShownHuds
                .filter { it.isVisibleEdit() && it.inBounds(lastMouseX, lastMouseY) }
                .forEach {
                    selectedHuds[it] = null
                    it.onMouseClick(mouseButtonEvent.x, mouseButtonEvent.y, mouseButtonEvent.button())
                }
            selectedHuds.entries.forEach {
                it.setValue(HudPosition(it.key.x, it.key.y))
            }
        } else {
            selectedHuds.clear()
            hoveredHud?.let {
                hoveredPos = HudPosition(it.x, it.y)
                it.onMouseClick(mouseButtonEvent.x, mouseButtonEvent.y, mouseButtonEvent.button())
            }
        }

        return false
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        mouseDown = false
        hoveredPos = null
        selectedHuds.entries.forEach { it.setValue(null) }

        return false
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        if (mouseButtonEvent.button() != 1) return false

        // updateSelected()
        if (selectedHuds.isEmpty()) {
            hoveredPos?.let { pos ->
                pos.cumDragX += d
                pos.cumDragY += e
                hoveredHud?.onMouseDrag(pos)
            }
        } else {
            selectedHuds.forEach { (hud, pos) ->
                if (pos == null) return@forEach
                pos.cumDragX += d
                pos.cumDragY += e
                hud.onMouseDrag(pos)
            }
        }

        return false
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        // updateSelected()
        if (selectedHuds.isEmpty()) {
            hoveredHud?.onMouseScroll(verticalAmount)
        } else {
            selectedHuds.forEach {
                it.key.onMouseScroll(verticalAmount)
            }
        }

        return false
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (keyEvent.key == InputConstants.KEY_ESCAPE) return super.keyPressed(keyEvent)

        if (selectedHuds.isEmpty()) {
            hoveredHud?.onKeyPress(keyEvent.key)
            // updateSelected()
        } else {
            selectedHuds.forEach {
                it.key.onKeyPress(keyEvent.key)
            }
        }

        return false
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        for (hud in allShownHuds) {
            if (hud.isEnabled()) continue
            if (hud.isInternal) continue
            if (!hud.isVisibleEdit()) continue
            hud.sampleDraw(graphics, mouseX, mouseY, hud == hoveredHud || hud in selectedHuds)
        }

        val window = minecraft.window
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

        for (hud in allShownHuds) {
            if (!hud.isEnabled() && !hud.isInternal) continue
            if (!hud.isVisibleEdit()) continue
            hud.sampleDraw(graphics, mouseX, mouseY, hud == hoveredHud || hud in selectedHuds)
        }
    }

    fun addHud(hud: HudFeature) {
        huds.add(hud)
    }
}