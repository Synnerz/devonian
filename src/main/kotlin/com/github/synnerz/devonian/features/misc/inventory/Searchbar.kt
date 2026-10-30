package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.BinaryOperators
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.EvaluationResult
import com.github.synnerz.devonian.utils.EvaluationResultType
import com.github.synnerz.devonian.utils.ExprTree
import com.github.synnerz.devonian.utils.ExpressionEvaluator
import com.github.synnerz.devonian.utils.ExpressionValidator
import com.github.synnerz.devonian.utils.StringSearcher
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.UnaryOperators
import com.github.synnerz.devonian.utils.render.states.QuadRenderState
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIText
import com.github.synnerz.talium.components.UITextInput
import com.github.synnerz.talium.effects.OutlineEffect
import com.github.synnerz.talium.utils.Renderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderPipelines
import org.joml.Matrix3x2f
import org.lwjgl.glfw.GLFW
import java.awt.Color

object Searchbar : HudFeature(
    "searchbar",
    "Searches the typed characters in the current container's items name and/or lore.",
    subcategory = "Inventory",
) {
    private val SETTING_BACKGROUND_COLOR = addColorPicker(
        "bgcolor",
        Color(50, 50, 50, 255).rgb,
        "Background color of the searchbar",
        "Background Color",
    )
    private val SETTING_UNFOCUSED_OPACITY = addDecimalSlider(
        "unfocusedOpacity",
        0.8,
        0.0, 1.0,
        "",
        "Unfocused Opacity",
    )
    private val SETTING_NAME_MATCH_COLOR = addColorPicker(
        "nameMatchColor",
        Color(0, 255, 0, 255).rgb,
        "Background color of the items which matched their name",
        "Name Match Color",
    )
    private val SETTING_LORE_MATCH_COLOR = addColorPicker(
        "loreMatchColor",
        Color(0, 255, 255, 255).rgb,
        "Background color of the items which matched their lore",
        "Lore Match Color",
    )
    private val SETTING_DISABLED_COLOR = addColorPicker(
        "disabledColor",
        Color(255, 0, 0, 255).rgb,
        "When double clicking the searchbar the highlight will be disabled, this color will be applied as outline",
        "Disable Highlight Color",
    )
    private val SETTING_DISABLE_HIGHLIGHT = addSwitch(
        "disableHighlight",
        true,
        "Disables the highlight if you double click on the searchbar after being focused",
        "Disable Highlight",
    )

    private var disableHighlight = false
    private val stateFocused = BasicState(false)
    private val stateBgColor = SETTING_BACKGROUND_COLOR.state.zip(SETTING_UNFOCUSED_OPACITY.state) { c, a ->
        c to ((((c ushr 24) * a).toInt() shl 24) or (c and 0xFFFFFF))
    }.zip(stateFocused) { (c1, c2), f -> if (f) c1 else c2 }.map { Color(it, true) }
    private val uiContainer = UIRect(x, y, 20.0, 5.0).apply {
        setColor(stateBgColor.value)
        addEffect(OutlineEffect(1.0, stateBgColor.value.darker()))
        stateBgColor.listen {
            setColor(it)
            if (disableHighlight) return@listen
            removeEffects(OutlineEffect::class.java)
            addEffect(OutlineEffect(1.0, stateBgColor.value.darker()))
        }


        var clickCount = 0
        var lastClickTime = 0L
        onMouseClick {
            if (!SETTING_DISABLE_HIGHLIGHT.get()) return@onMouseClick
            if (it.button != 0) return@onMouseClick

            val t = System.currentTimeMillis()
            if (t - lastClickTime > 500L) clickCount = 0
            lastClickTime = t

            if (++clickCount < 2) return@onMouseClick
            clickCount = 0

            if (disableHighlight) {
                removeEffects(OutlineEffect::class.java)
                addEffect(OutlineEffect(1.0, stateBgColor.value.darker()))
                disableHighlight = false
            } else {
                removeEffects(OutlineEffect::class.java)
                addEffect(OutlineEffect(2.0, SETTING_DISABLED_COLOR.getColor()))
                disableHighlight = true
            }
        }

        onFocus { event ->
            stateFocused.value = true
            input.propagateFocus(event)
        }

        onUnfocus {
            stateFocused.value = false
        }

        onResize { _, _ -> onResize() }
    }
    private val input: UITextInput = UITextInput(0.0, 0.0, 85.0, 100.0, parent = uiContainer).apply {
        onCharType {
            onKeyType()
        }

        onUnfocus {
            uiContainer.unfocus()
        }
    }
    private val infoText = UIText(87.5, 0.0, 12.5, 100.0, parent = uiContainer, centered = true)

    private var highlightItems = emptyList<MatchType>()

    override fun getBounds(): BoundingBox {
        uiContainer.checkUpdate()
        val w = uiContainer.width * scale
        val h = uiContainer.height * scale
        return BoundingBox(x, y, w, h)
    }

    override fun drawImpl(ctx: GuiGraphicsExtractor) {
        uiContainer.checkUpdate()

        Renderer.stack
            .pushMatrix()
            .translate(x.toFloat(), y.toFloat())
            .scale(scale)
            // .translate(-uiContainer.width.toFloat() * 0.5f, -uiContainer.height.toFloat() * 0.5f)

        uiContainer.draw(uiContainer.x, uiContainer.y)

        Renderer.stack.popMatrix()
    }

    override fun sampleDraw(ctx: GuiGraphicsExtractor, mx: Int, my: Int, selected: Boolean) {
        val pos = getBounds()

        ctx.guiRenderState.addGuiElement(
            QuadRenderState(
                RenderPipelines.GUI,
                Matrix3x2f(ctx.pose()),
                pos.x.toFloat(), pos.y.toFloat(),
                (pos.x + pos.w).toFloat(), (pos.y + pos.h).toFloat(),
                SETTING_BACKGROUND_COLOR.get(),
                ctx.scissorStack.peek(),
            )
        )
        super.sampleDraw(ctx, mx, my, selected)
    }

    override fun initialize() {
        on<PostRenderGuiEvent> {
            if (it.screen !is AbstractContainerScreen<*>) return@on

            draw(it.ctx)
        }

        on<ClientContainerCloseEvent> {
            highlightItems = emptyList()
        }

        on<ServerContainerCloseEvent> {
            highlightItems = emptyList()
        }

        on<GuiKeyDownEvent> { event ->
            if (event.screen !is AbstractContainerScreen<*>) return@on
            if ((event.event.modifiers and 2) != 0 && event.key == GLFW.GLFW_KEY_F)
                uiContainer.focused = true
            if (!uiContainer.focused) return@on

            uiContainer.handleKeyInput(event.key, event.scanCode)
            event.cancel()
        }

        on<GuiCharTypeEvent> { event ->
            val screen = minecraft.gui.screen() ?: return@on
            if (screen !is AbstractContainerScreen<*>) return@on
            if (!uiContainer.focused) return@on

            uiContainer.handleCharType(event.codepoint, event.str, -1)
            event.cancel()
        }

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            val color = when (highlightItems.getOrNull(slot.index) ?: return@on) {
                MatchType.NONE -> return@on
                MatchType.LORE -> SETTING_LORE_MATCH_COLOR.get()
                MatchType.NAME -> SETTING_NAME_MATCH_COLOR.get()
            }

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color)
        }.prio = 30

        on<TickEvent> { event ->
            if (event.tick % 5 != 1) return@on
            val screen = minecraft.gui.screen() ?: return@on
            if (screen !is AbstractContainerScreen<*>) return@on

            updateItems()
        }
    }

    private fun resetGuiLocation() {
        uiContainer._x = (x / window.guiScaledWidth) * 100
        uiContainer._y = (y / window.guiScaledHeight) * 100
        uiContainer.setDirty()
    }

    override fun onMouseDrag(pos: HudManager.HudPosition) {
        super.onMouseDrag(pos)
        resetGuiLocation()
    }

    override fun onKeyPress(keyCode: Int) {
        super.onKeyPress(keyCode)
        resetGuiLocation()
    }

    private fun onResize() {
        resetGuiLocation()
    }

    private val operandExtractor = ExpressionEvaluator.wrap(
        { listOf(it) },
        UnaryOperators.entries.associateWith { { it } },
        BinaryOperators.entries.associateWith { { a, b -> a + b } },
    )

    private val validator = ExpressionValidator(
        EvaluationResultType.Either,
        mapOf(
            UnaryOperators.LogicalNot to (EvaluationResultType.Computed to EvaluationResultType.Computed),
            UnaryOperators.GreaterThan to (EvaluationResultType.Numeric to EvaluationResultType.Computed),
            UnaryOperators.LessThan to (EvaluationResultType.Numeric to EvaluationResultType.Computed),
        ),
        mapOf(
            BinaryOperators.LogicalAnd to (EvaluationResultType.Computed to EvaluationResultType.Computed),
            BinaryOperators.LogicalOr to (EvaluationResultType.Computed to EvaluationResultType.Computed),
        ),
        ExpressionValidator.Numeric,
    )
    private val baseEvaluator = ExpressionEvaluator.wrap<Boolean>(
        null,
        mapOf(
            UnaryOperators.LogicalNot to { !it },
        ),
        mapOf(
            BinaryOperators.LogicalAnd to { a, b -> a && b },
            BinaryOperators.LogicalOr to { a, b -> a || b },
        ),
        ExpressionEvaluator.getNumeric1(),
    )

    private var cachedExp: ExprTree? = null
    private var searcher: StringSearcher? = null
    private fun onKeyType() {
        val text = input.text

        cachedExp = null
        searcher = null
        highlightItems = emptyList()
        infoText.text = ""

        if (text.isEmpty()) return

        var error: String? = null
        var exp =
            if (text.isBlank()) null
            else ExprTree.parse(text.lowercase()).also {
                if (it == null) error = ":("
            }

        exp?.evaluate(ExpressionEvaluator.Numeric, ExpressionValidator.Numeric)?.let {
            infoText.setColor(Color(100, 100, 100))
            infoText.text = StringUtils.formatShortest(it.getNumeric(), 5)
            return
        }

        if (exp != null && !exp.validate(validator)) {
            error = ":("
            exp = null
        }

        searcher =
            if (disableHighlight && SETTING_DISABLE_HIGHLIGHT.get()) null
            else exp?.evaluate(operandExtractor, ExpressionValidator.Computed)?.let {
                StringSearcher(it.getComputed())
            } ?: run {
                error = ":("
                exp = null
                null
            }
        searcher?.build()

        if (error != null) {
            infoText.setColor(Color(200, 50, 50))
            infoText.text = error
        }

        cachedExp = exp

        updateItems()
    }

    private fun matchesText(exp: ExprTree, matches: Set<String>, itemValue: Double): Boolean {
        val evaluator = ExpressionEvaluator(
            { matches.contains(it) },
            mapOf(
                UnaryOperators.GreaterThan to { EvaluationResult.ComputedResult(it.getNumeric() > itemValue) },
                UnaryOperators.LessThan to { EvaluationResult.ComputedResult(it.getNumeric() < itemValue) },
            ),
            null,
            baseEvaluator,
        )

        return exp.evaluate(evaluator, validator)!!.getComputed()
    }

    private fun updateItems() {
        highlightItems = emptyList()

        if (disableHighlight && SETTING_DISABLE_HIGHLIGHT.get()) return

        val screen = minecraft.gui.screen() ?: return
        val container = screen as? AbstractContainerScreen<*> ?: return
        val items = container.menu.items
        val exp = cachedExp
        val s = searcher

        highlightItems = items.map { item ->
            if (exp == null) return@map MatchType.NONE
            if (s == null) return@map MatchType.NONE
            if (item.isEmpty) return@map MatchType.NONE

            val sbId = ItemUtils.skyblockId(item)
            val itemValue = (sbId?.let { SkyblockPrices.buyPrice(it) } ?: -1f).toDouble()

            val itemName = item.hoverName.string
            val nameMatches = s.search(itemName.lowercase()).map { it.pattern }.toSet()

            if (matchesText(exp, nameMatches, itemValue)) return@map MatchType.NAME

            val loreMatches = mutableSetOf<String>()
            val itemLore = ItemUtils.lore(item)
            itemLore?.forEach {
                s.search(it.lowercase()).forEach {
                    loreMatches.add(it.pattern)
                }
            }

            return@map if (matchesText(exp, loreMatches, itemValue)) MatchType.LORE else MatchType.NONE
        }
    }

    enum class MatchType {
        NAME,
        LORE,
        NONE;
    }
}