package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.events.DropItemEvent;
import com.github.synnerz.devonian.events.EventBus;
import com.github.synnerz.devonian.events.GuiSlotClickEvent;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Shadow
    @Final
    protected ScreenHandler handler;

    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V"),
            cancellable = true
    )
    private void devonian$onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        EventBus.INSTANCE.post(new GuiSlotClickEvent(slot, slotId, button, actionType, ci));
    }

    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onDropItem(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (
                (slotId != -999 && actionType == SlotActionType.THROW) ||
                (slotId == -999 && actionType == SlotActionType.PICKUP)
        ) {
            ItemStack stack = handler.getCursorStack();
            if (!stack.isEmpty()) {
                DropItemEvent event = new DropItemEvent(stack, button == 0);
                EventBus.INSTANCE.post(event);
                if (event.isCancelled()) ci.cancel();
            } else if (slot != null) {
                ItemStack slotStack = slot.getStack();
                if (!slotStack.isEmpty()) {
                    DropItemEvent event = new DropItemEvent(slotStack, button == 0);
                    EventBus.INSTANCE.post(event);
                    if (event.isCancelled()) ci.cancel();
                }
            }
        }
    }
}
