package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devonian.api.events.*;
import com.github.synnerz.devonian.features.misc.DisableGlassPaneHighlight;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
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
    private void devonian$onSlotClicked(Slot slot, int i, int j, ClickType clickType, CallbackInfo ci) {
        CancellableEvent event = null;

        AbstractContainerScreen<?> that = (AbstractContainerScreen<?>) (Object) this;

        switch (clickType) {
            case PICKUP:
                if (slot == null) event = new DropItemEvent(null, true, menu.getCarried(), true);
                else event = new PickupItemInventoryEvent(slot, that, j == 1, false);
                break;
            case THROW:
                event = new DropItemEvent(slot, j != 0, slot == null ? ItemStack.EMPTY : slot.getItem(), true);
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
        if (new RenderSlotEvent(slot, guiGraphics, instance).post()) return;
        original.call(instance, guiGraphics, slot);
    }

    @Inject(
        method = "renderContents",
        at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;popMatrix()Lorg/joml/Matrix3x2fStack;", remap = false)
    )
    private void devonian$postRenderSlots(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        new PostRenderSlotsEvent(guiGraphics, i, j, (AbstractContainerScreen<?>) (Object) this).post();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void devonian$renderContainer(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (!(Devonian.INSTANCE.getMinecraft().screen instanceof ContainerScreen)) return;

        if (new ContainerRenderEvent((ContainerScreen) Devonian.INSTANCE.getMinecraft().screen, i, j, f, guiGraphics).post())
            ci.cancel();
    }

    @Unique
    private final Set<Item> panes = Set.of(
        Items.GLASS_PANE,
        Items.WHITE_STAINED_GLASS_PANE,
        Items.ORANGE_STAINED_GLASS_PANE,
        Items.MAGENTA_STAINED_GLASS_PANE,
        Items.LIGHT_BLUE_STAINED_GLASS_PANE,
        Items.YELLOW_STAINED_GLASS_PANE,
        Items.LIME_STAINED_GLASS_PANE,
        Items.PINK_STAINED_GLASS_PANE,
        Items.GRAY_STAINED_GLASS_PANE,
        Items.LIGHT_GRAY_STAINED_GLASS_PANE,
        Items.CYAN_STAINED_GLASS_PANE,
        Items.PURPLE_STAINED_GLASS_PANE,
        Items.BLUE_STAINED_GLASS_PANE,
        Items.BROWN_STAINED_GLASS_PANE,
        Items.GREEN_STAINED_GLASS_PANE,
        Items.RED_STAINED_GLASS_PANE,
        Items.BLACK_STAINED_GLASS_PANE
    );

    @Inject(
        method = "renderSlotHighlightBack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V"),
        cancellable = true
    )
    private void devonian$test1(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!DisableGlassPaneHighlight.INSTANCE.isEnabled()) return;
        assert hoveredSlot != null;
        ItemStack item = hoveredSlot.getItem();
        if (!panes.contains(item.getItem())) return;
        if (!item.getHoverName().getString().isBlank()) return;
        ci.cancel();
    }

    @Inject(
        method = "renderSlotHighlightFront",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V"),
        cancellable = true
    )
    private void devonian$test2(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!DisableGlassPaneHighlight.INSTANCE.isEnabled()) return;
        assert hoveredSlot != null;
        ItemStack item = hoveredSlot.getItem();
        if (!panes.contains(item.getItem())) return;
        if (!item.getHoverName().getString().isBlank()) return;
        ci.cancel();
    }
}
