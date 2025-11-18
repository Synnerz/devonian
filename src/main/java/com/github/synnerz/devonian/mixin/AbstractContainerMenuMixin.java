package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.DropItemEvent;
import com.github.synnerz.devonian.api.events.EventBus;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {
    @Inject(method = "clearContainer", at = @At("HEAD"), cancellable = true)
    private void devonian$screenDropInventory(Player player, Container container, CallbackInfo ci) {
        if (container != player.inventoryMenu.getCraftSlots()) {
            for (int idx = 0; idx < container.getContainerSize(); idx++) {
                ItemStack stack = container.getItem(idx);
                DropItemEvent event = new DropItemEvent(stack, true);
                if (!stack.isEmpty()) EventBus.INSTANCE.post(event);
                if (event.isCancelled()) ci.cancel();
            }
        }
    }
}
