package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIText
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component

object ConfigGui : Screen(Component.literal("Devonian.ConfigGui")) {
    private val background = UIRect(0.0, 0.0, 100.0, 100.0)
    private val main = UIRect(20.0, 15.0, 60.0, 60.0, parent = background).apply {
        setColor(ColorPalette.PRIMARY_COLOR)
    }
    private val leftPanel = UIRect(0.0, 0.0, 20.0, 100.0, parent = main).apply {
        setColor(ColorPalette.SECONDARY_COLOR)
    }
    private val rightPanel = UIRect(21.0, 1.0, 78.5, 98.0, parent = main).apply {
        setColor(ColorPalette.SECONDARY_COLOR)
    }
    private val configTitle = UIText(0.0, 0.0, 100.0, 10.0,"Devonian", true, leftPanel).apply {
        setColor(ColorPalette.TITLE_COLOR)
    }
    private val editHudsButton = UIRect(32.5, 90.0, 35.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Edit Huds", true).apply {
            setColor(ColorPalette.TEXT_COLOR)
        })
        onMouseRelease {
            if (it.button != 0) return@onMouseRelease
            Scheduler.scheduleTask(1) {
                Devonian.minecraft.setScreen(HudManager)
            }
        }
    }
    lateinit var categories: List<Category>
    lateinit var selectedCategory: Category

    fun category(name: String): Category {
        return categories.find { it.categoryName.equals(name, ignoreCase = true) }!!
    }

    fun initialize() {
        categories = Config.categories.keys.map { Category(it, rightPanel, leftPanel) }
        selectedCategory = categories.first()
        selectedCategory.unhide()
        background.onMouseScroll {
            if (!selectedCategory.canTrigger())
                selectedCategory.hideColorPickers()
            selectedCategory.onMouseScroll(it.delta)
        }

        selectedCategory.update()
        DevonianCommand.onRun {
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreen(this)
            }
            return@onRun 1
        }
        DevonianCommand.command.subcommand("configui") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreen(this)
            }
            return@subcommand 1
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.render(context, mouseX, mouseY, deltaTicks)
        background.draw()
    }

    override fun renderBackground(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        // no background here bud
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        background.handleKeyInput(keyEvent.key, keyEvent.scancode)
        return super.keyPressed(keyEvent)
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun onClose() {
        selectedCategory.hideColorPickers()
        super.onClose()
    }
}