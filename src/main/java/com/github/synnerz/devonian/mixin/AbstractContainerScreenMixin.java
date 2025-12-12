package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.DropItemEvent;
import com.github.synnerz.devonian.api.events.EventBus;
import com.github.synnerz.devonian.api.events.GuiSlotClickEvent;
import com.github.synnerz.devonian.api.events.RenderSlotEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerScreen.class, priority = 1002)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Shadow protected abstract void renderSlot(GuiGraphics context, Slot slot);

    @Inject(
            method = "slotClicked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleInventoryMouseClick(IIILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"),
            cancellable = true
    )
    private void devonian$onSlotClick(Slot slot, int i, int j, ClickType clickType, CallbackInfo ci) {
        GuiSlotClickEvent event = new GuiSlotClickEvent(slot, i, j, clickType, menu);
        EventBus.INSTANCE.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(
            method = "slotClicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onDropItem(Slot slot, int i, int j, ClickType clickType, CallbackInfo ci) {
        if (
                (i != -999 && clickType == ClickType.THROW) ||
                (i == -999 && clickType == ClickType.PICKUP)
        ) {
            ItemStack stack = menu.getCarried();
            if (!stack.isEmpty()) {
                DropItemEvent event = new DropItemEvent(stack, j == 0);
                EventBus.INSTANCE.post(event);
                if (event.isCancelled()) ci.cancel();
            } else if (slot != null) {
                ItemStack slotStack = slot.getItem();
                if (!slotStack.isEmpty()) {
                    DropItemEvent event = new DropItemEvent(slotStack, j == 0);
                    EventBus.INSTANCE.post(event);
                    if (event.isCancelled()) ci.cancel();
                }
            }
        }
    }

    @Redirect(
            method = "renderSlots",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V"
            )
    )
    private void devonian$drawSlots(AbstractContainerScreen instance, GuiGraphics guiGraphics, Slot slot) {
        if (new RenderSlotEvent(slot, guiGraphics).post()) return;

        renderSlot(guiGraphics, slot);
    }
}
