package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState
import net.minecraft.world.item.ItemStack
import java.util.WeakHashMap

object HighlightDroppedItems : Feature("highlightDroppedItems") {
    private val items = WeakHashMap<ItemStack, Int>()

    fun extractItemCluster(item: ItemStack, state: ItemClusterRenderState) {
        if (state.outlineColor != 0) return
        state.outlineColor = items.getOrPut(item) {
            item.customName?.siblings?.getOrNull(0)?.style?.color?.value ?: -1
        }
    }
}