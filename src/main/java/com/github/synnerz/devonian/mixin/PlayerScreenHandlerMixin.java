package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.events.DropItemEvent;
import com.github.synnerz.devonian.events.EventBus;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin {
    @Shadow
    public abstract RecipeInputInventory getCraftingInput();

    @Inject(
            method = "onClosed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/inventory/CraftingResultInventory;clear()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void devonian$onClosedClear(PlayerEntity player, CallbackInfo ci) {
        if (player.getWorld().isClient) {
            RecipeInputInventory craftingInventory = getCraftingInput();
            for (int idx = 0; idx < craftingInventory.size(); idx++) {
                ItemStack stack = craftingInventory.getStack(idx);
                DropItemEvent event = new DropItemEvent(stack, true);
                if (!stack.isEmpty()) EventBus.INSTANCE.post(event);
                if (event.isCancelled()) ci.cancel();
            }
        }
    }
}
