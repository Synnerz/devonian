package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.PostExtractRenderEntityEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import java.awt.Color
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.sin

object HighlightDroppedItems : Feature("highlightDroppedItems") {
    private val SETTING_BOX = addSwitch(
        "box",
        false,
        "box instead of outline",
        "Item Highlight Box",
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        1.0,
        0.0, 10.0,
        "",
        "Item Box Line Width",
    )
    private val SETTING_BOX_ALPHA = addDecimalSlider(
        "boxAlpha",
        1.0,
        0.0, 1.0,
        "",
        "Item Box Wire Alpha",
    )
    private val SETTING_FILL_ALPHA = addDecimalSlider(
        "fillAlpha",
        0.5,
        0.0, 1.0,
        "",
        "Item Box Fill Alpha",
    )
    private val SETTING_DISPLAY_NAME = addSwitch(
        "displayName",
        false,
        "Displays the item's name and stack size",
        "Display Name"
    )

    private val items = WeakHashMap<ItemStack, Int>()
    private val deferredItems = mutableListOf<Triple<Triple<Double, Double, Double>, Int, WeakReference<ItemStack>>>()

    override fun initialize() {
        on<PostExtractRenderEntityEvent> { event ->
            val state = event.state as? ItemEntityRenderState ?: return@on
            val item = (event.entity as? ItemEntity)?.item ?: return@on

            val color = items.getOrPut(item) {
                item.customName?.style?.color?.value ?: -1
            }
            if (SETTING_BOX.get()) {
                deferredItems.add(
                    Triple(
                        Triple(
                            state.x,
                            state.y + sin(state.ageInTicks * 0.1 + state.bobOffset) * 0.1 + 0.1625,
                            state.z
                        ),
                        color,
                        WeakReference(item),
                    )
                )
            } else if (state.outlineColor == 0) state.outlineColor = color
        }

        on<RenderWorldEvent> {
            deferredItems.forEach { (pos, col, ref) ->
                val (x, y, z) = pos
                Render3DImmediate.renderWireframeBox(
                    x, y, z,
                    0.3, 0.3,
                    Color(col or ((SETTING_BOX_ALPHA.get() * 255.0).toInt() shl 24), true),
                    phase = true,
                    lineWidth = SETTING_LINE_WIDTH.get(),
                    centered = true,
                )
                Render3DImmediate.renderFilledBox(
                    x, y, z,
                    0.3, 0.3,
                    Color(col or ((SETTING_FILL_ALPHA.get() * 255.0).toInt() shl 24), true),
                    centered = true,
                )

                if (!SETTING_DISPLAY_NAME.get()) return@forEach
                val item = ref.get() ?: return@forEach
                val name = item.customName ?: return@forEach
                Render3DImmediate.renderString(
                    "&ax${item.count} &f${name.colorCodes()}".replaceCodes(),
                    x, y + 0.8, z,
                    phase = true,
                )
            }
            deferredItems.clear()
        }.setEnabled(SETTING_BOX.state)
    }
}