package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.events.EventBus;
import com.github.synnerz.devonian.events.GuiSlotClickEvent;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V"),
            cancellable = true
    )
    private void devonian$onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        EventBus.INSTANCE.post(new GuiSlotClickEvent(slot, slotId, button, actionType, ci));
    }
}
