package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devonian.api.events.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = AbstractContainerScreen.class, priority = 1002)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Inject(
        method = "slotClicked",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$onSlotClicked(Slot slot, int i, int j, ClickType clickType, CallbackInfo ci) {
        CancellableEvent event = null;

        AbstractContainerScreen<?> that = (AbstractContainerScreen<?>) (Object) this;

        switch (clickType) {
            case PICKUP:
                if (slot == null) event = new DropItemEvent(null, true, menu.getCarried(), true);
                else event = new PickupItemInventoryEvent(slot, that, j == 1);
                break;
            case THROW:
                event = new DropItemEvent(slot, j != 0, slot.getItem(), true);
                break;
            case PICKUP_ALL:
                if (slot != null) event = new PickupItemInventoryEvent(slot, that, false);
                break;
            case QUICK_MOVE:
                if (slot != null) event = new QuickMoveItemEvent(slot, that);
                break;
            case SWAP: {
                if (slot == null) break;
                Player player = Devonian.INSTANCE.getMinecraft().player;
                if (player == null) break;
                Inventory inv = player.getInventory();
                Optional<Slot> other = menu.slots.stream().filter(v -> v.container == inv && v.getContainerSlot() == j).findAny();
                if (other.isPresent()) event = new SwapItemEvent(slot, other.get());
                break;
            }
            case QUICK_CRAFT:
                // leftover item, into the cursor slot
                if (slot == null) break;
                event = new QuickCraftMoveEvent(slot, (j & 4) > 0, that);
                break;
        }

        if (event != null && event.post()) ci.cancel();
    }

    @WrapOperation(
        method = "renderSlots",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V"
        )
    )
    private void devonian$drawSlots(AbstractContainerScreen instance, GuiGraphics guiGraphics, Slot slot, Operation<Void> original) {
        if (new RenderSlotEvent(slot, guiGraphics).post()) return;
        original.call(instance, guiGraphics, slot);
    }

    @Inject(
        method = "renderContents",
        at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;popMatrix()Lorg/joml/Matrix3x2fStack;", remap = false)
    )
    private void devonian$postRenderSlots(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        new PostRenderSlotsEvent(guiGraphics, i, j, (AbstractContainerScreen<?>) (Object) this).post();
    }
}
