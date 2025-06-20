package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.events.DropItemEvent;
import com.github.synnerz.devonian.events.EventBus;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    private void devonian$screenDropInventory(PlayerEntity player, Inventory inventory, CallbackInfo ci) {
        if (inventory != player.playerScreenHandler.getCraftingInput()) {
            for (int idx = 0; idx < inventory.size(); idx++) {
                ItemStack stack = inventory.getStack(idx);
                DropItemEvent event = new DropItemEvent(stack, true);
                if (!stack.isEmpty()) EventBus.INSTANCE.post(event);
                if (event.isCancelled()) ci.cancel();
            }
        }
    }
}
