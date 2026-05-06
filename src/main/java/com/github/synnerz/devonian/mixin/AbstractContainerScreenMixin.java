package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devonian.api.events.*;
import com.github.synnerz.devonian.features.misc.DisableGlassPaneHighlight;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Set;

@Mixin(value = AbstractContainerScreen.class, priority = 1002)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Inject(
        method = "slotClicked",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$onSlotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
        CancellableEvent event = null;

        AbstractContainerScreen<?> that = (AbstractContainerScreen<?>) (Object) this;

        switch (containerInput) {
            case PICKUP:
                if (slot == null) event = new DropItemEvent(null, true, menu.getCarried(), true);
                else event = new PickupItemInventoryEvent(slot, that, buttonNum == 1, false);
                break;
            case THROW:
                event = new DropItemEvent(slot, buttonNum != 0, slot == null ? ItemStack.EMPTY : slot.getItem(), true);
                break;
            case PICKUP_ALL:
                if (slot != null) event = new PickupItemInventoryEvent(slot, that, false, true);
                break;
            case QUICK_MOVE:
                if (slot != null) event = new QuickMoveItemEvent(slot, that);
                break;
            case SWAP: {
                if (slot == null) break;
                Player player = Devonian.INSTANCE.getMinecraft().player;
                if (player == null) break;
                Inventory inv = player.getInventory();
                Optional<Slot> other = menu.slots.stream().filter(v -> v.container == inv && v.getContainerSlot() == buttonNum).findAny();
                if (other.isPresent()) event = new SwapItemEvent(slot, other.get(), that);
                break;
            }
            case QUICK_CRAFT:
                // leftover item, into the cursor slot
                if (slot == null) break;
                event = new QuickCraftMoveEvent(slot, (buttonNum & 4) > 0, that);
                break;
        }

        if (event != null && event.post()) ci.cancel();
    }

    @WrapOperation(
        method = "extractSlots",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V"
        )
    )
    private void devonian$drawSlots(AbstractContainerScreen instance, GuiGraphicsExtractor carry, Slot newCount, int icon, int seed, Operation<Void> original) {
        if (new RenderSlotEvent(newCount, carry, instance).post()) return;
        original.call(instance, carry, newCount, icon, seed);
        new PostRenderSlotEvent(newCount, carry, instance).post();
    }

    @Inject(
        method = "extractContents",
        at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;popMatrix()Lorg/joml/Matrix3x2fStack;", remap = false)
    )
    private void devonian$postRenderSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        new PostRenderSlotsEvent(graphics, mouseX, mouseY, (AbstractContainerScreen<?>) (Object) this).post();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void devonian$renderContainer(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!(Devonian.INSTANCE.getMinecraft().gui.screen() instanceof ContainerScreen)) return;

        if (new ContainerRenderEvent((ContainerScreen) Devonian.INSTANCE.getMinecraft().gui.screen(), mouseX, mouseY, a, graphics).post())
            ci.cancel();
    }

    @Unique
    private final Set<Item> panes = Set.of(
        Items.GLASS_PANE,
        Items.STAINED_GLASS_PANE.white(),
        Items.STAINED_GLASS_PANE.orange(),
        Items.STAINED_GLASS_PANE.magenta(),
        Items.STAINED_GLASS_PANE.lightBlue(),
        Items.STAINED_GLASS_PANE.yellow(),
        Items.STAINED_GLASS_PANE.lime(),
        Items.STAINED_GLASS_PANE.pink(),
        Items.STAINED_GLASS_PANE.gray(),
        Items.STAINED_GLASS_PANE.lightGray(),
        Items.STAINED_GLASS_PANE.cyan(),
        Items.STAINED_GLASS_PANE.purple(),
        Items.STAINED_GLASS_PANE.blue(),
        Items.STAINED_GLASS_PANE.brown(),
        Items.STAINED_GLASS_PANE.green(),
        Items.STAINED_GLASS_PANE.red(),
        Items.STAINED_GLASS_PANE.black()
    );

    @Inject(
        method = "extractSlotHighlightBack",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
        ),
        cancellable = true
    )
    private void devonian$onSlotHighlightBack(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!DisableGlassPaneHighlight.INSTANCE.isEnabled()) return;
        assert hoveredSlot != null;
        ItemStack item = hoveredSlot.getItem();
        if (!panes.contains(item.getItem())) return;
        if (!item.getHoverName().getString().isBlank()) return;
        ci.cancel();
    }

    @Inject(
        method = "extractSlotHighlightFront",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
        ),
        cancellable = true
    )
    private void devonian$onSlotHighlightFront(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!DisableGlassPaneHighlight.INSTANCE.isEnabled()) return;
        assert hoveredSlot != null;
        ItemStack item = hoveredSlot.getItem();
        if (!panes.contains(item.getItem())) return;
        if (!item.getHoverName().getString().isBlank()) return;
        ci.cancel();
    }
}
