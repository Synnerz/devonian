package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.DropItemEvent;
import com.github.synnerz.devonian.api.events.EventBus;
import com.github.synnerz.devonian.api.events.GuiSlotClickEvent;
import com.github.synnerz.devonian.api.events.RenderSlotEvent;
import net.minecraft.client.gui.DrawContext;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    @Final
    protected ScreenHandler handler;

    @Shadow protected abstract void drawSlot(DrawContext context, Slot slot);

    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V"),
            cancellable = true
    )
    private void devonian$onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        GuiSlotClickEvent event = new GuiSlotClickEvent(slot, slotId, button, actionType, handler);
        EventBus.INSTANCE.post(event);
        if (event.isCancelled()) ci.cancel();
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

    @Redirect(
            method = "drawSlots",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V")
    )
    private void devonian$drawSlots(HandledScreen instance, DrawContext context, Slot slot) {
        if (new RenderSlotEvent(slot, context).post()) return;

        drawSlot(context, slot);
    }
}
