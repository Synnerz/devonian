package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.DropItemEvent;
import com.github.synnerz.devonian.api.events.EventBus;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LocalPlayer.class, priority = 1002)
public class LocalPlayerMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void devonian$dropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        int index = InventoryMenu.USE_ROW_SLOT_START + player.getInventory().getSelectedSlot();
        Slot slot = player.inventoryMenu.getSlot(index);

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (new DropItemEvent(slot, entireStack, stack).post()) cir.setReturnValue(false);
    }
}
