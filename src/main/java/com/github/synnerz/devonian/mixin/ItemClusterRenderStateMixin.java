package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.HighlightDroppedItems;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemClusterRenderState.class)
public class ItemClusterRenderStateMixin {
    @Inject(
        method = "extractItemGroupRenderState",
        at = @At("HEAD")
    )
    private void devonian$highlightDroppedItems(Entity entity, ItemStack itemStack, ItemModelResolver itemModelResolver, CallbackInfo ci) {
        if (!HighlightDroppedItems.INSTANCE.isEnabled()) return;
        ItemClusterRenderState state = (ItemClusterRenderState) (Object) this;
        HighlightDroppedItems.INSTANCE.extractItemCluster(itemStack, state);
    }
}
