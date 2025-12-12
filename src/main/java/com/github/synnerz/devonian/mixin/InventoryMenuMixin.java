package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.DropItemEvent;
import com.github.synnerz.devonian.api.events.EventBus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InventoryMenu.class, priority = 1002)
public abstract class InventoryMenuMixin {
    @Shadow
    public abstract CraftingContainer getCraftSlots();

    @Inject(
            method = "removed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/ResultContainer;clearContent()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void devonian$onClosedClear(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) {
            CraftingContainer craftingInventory = getCraftSlots();
            for (int idx = 0; idx < craftingInventory.getContainerSize(); idx++) {
                ItemStack stack = craftingInventory.getItem(idx);
                DropItemEvent event = new DropItemEvent(stack, true);
                if (!stack.isEmpty()) EventBus.INSTANCE.post(event);
                if (event.isCancelled()) ci.cancel();
            }
        }
    }
}
