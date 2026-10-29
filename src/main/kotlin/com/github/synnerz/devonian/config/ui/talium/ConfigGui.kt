package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.talium.components.UIBase
import com.github.synnerz.talium.components.UIElement
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIScrollable
import com.github.synnerz.talium.components.UIText
import com.github.synnerz.talium.events.UIEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import java.awt.Color

object ConfigGui : Screen(Component.literal("Devonian.ConfigGui")) {
    private val background = object : UIRect(0.0, 0.0, 100.0, 100.0) {
        override fun onPreChildPropagate(event: UIEvent, child: UIElement): Boolean {
            val modal = currentModal
            if (modal?.hidden != false) return super.onPreChildPropagate(event, child)

            return child === modal
        }
    }
    private val main = UIRect(17.5, 17.5, 65.0, 65.0, parent = background).apply {
        setColor(ColorPalette.PRIMARY_COLOR)
    }
    private val leftPanel = UIRect(0.0, 0.0, 20.0, 100.0, parent = main).apply {
        setColor(ColorPalette.SECONDARY_COLOR)
        addChild(
            UIText(12.0, 4.0, 100.0, 10.0, "Devonian - v${Devonian.DEVONIAN_VERSION}", false).apply {
                setColor(ColorPalette.TITLE_COLOR)
                onResize { _, w ->
                    textScale = 2f / w.scaleFactor
                }
            }
        )
    }
    private val leftPanelScroll = UIScrollable(0.0, 12.0, 100.0, 79.0, parent = leftPanel).apply {
        drawScrollbar = true
        scrollBgColor = Color(35, 35, 35, 255)
        scrollFgColor = Color(70, 70, 70, 255)
    }
    private val rightPanel = UIRect(21.0, 1.0, 78.5, 98.0, parent = main).apply {
        setColor(ColorPalette.SECONDARY_COLOR)
    }
    private val editHudsBtn = UIRect(1.5, 92.0, 97.0, 7.0, parent = leftPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(
            UIText(0.0, 0.0, 100.0, 100.0, "Edit Huds", true).apply {
                setColor(ColorPalette.TEXT_COLOR)
                onResize {  _, w ->
                    textScale = 2.5f / w.scaleFactor
                }
            }
        )

        onMouseRelease {
            if (it.button != 0) return@onMouseRelease
            Scheduler.scheduleTask(1) {
                Devonian.minecraft.setScreenAndShow(HudManager)
            }
        }
    }

    private var currentModal: UIBase? = null
    lateinit var categories: List<Category>
    var selectedCategory: Category? = null
    lateinit var searchCategory: SearchCategory
    var opened = false

    fun initialize() {
        categories = Config.categories.keys.map {
            Category(it, rightPanel, leftPanelScroll)
        }
        searchCategory = SearchCategory(rightPanel)
        selectedCategory = categories.first()
        selectedCategory?.unhide()

        DevonianCommand.onRun {
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreenAndShow(this)
                background.focus()
                searchCategory.searchbar.focus()
            }
            return@onRun 1
        }
        DevonianCommand.command.subcommand("configui") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreenAndShow(this)
                background.focus()
                searchCategory.searchbar.focus()

            }
            return@subcommand 1
        }
    }

    fun requestModal(ui: UIBase?) {
        currentModal = ui
        ui?.unhide()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)
        background.draw()
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        background.handleCharType(characterEvent.codepoint, characterEvent.codepointAsString(), -1)
        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        background.handleKeyInput(keyEvent.key, keyEvent.scancode)
        if (keyEvent.isEscape && background.unfocusKeybinds()) return false
        return super.keyPressed(keyEvent)
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun removed() {
        currentModal?.hide()
        currentModal = null
        background.hide()
        opened = false
    }

    override fun added() {
        background.unhide()
        opened = true
    }
}