package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.CustomContainerColor;
import com.github.synnerz.devonian.features.misc.HideCraftingText;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
    public InventoryScreenMixin(InventoryMenu recipeBookMenu, RecipeBookComponent<?> recipeBookComponent, Inventory inventory, Component component) {
        super(recipeBookMenu, recipeBookComponent, inventory, component);
    }

    @WrapOperation(
        method = "renderBg",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V")
    )
    private void devonian$renderBg(GuiGraphics instance, RenderPipeline renderPipeline, Identifier identifier, int i, int j, float f, float g, int k, int l, int m, int n, Operation<Void> original) {
        if (!CustomContainerColor.INSTANCE.isEnabled()) {
            original.call(instance, renderPipeline, resourceLocation, i, j, f, g, k, l, m, n);
            return;
        }

        int color = CustomContainerColor.INSTANCE.getSETTING_CONTAINER_COLOR().get();
        instance.blit(
            renderPipeline,
            identifier,
            i,
            j,
            0.0f,
            0.0f,
            imageWidth,
            imageHeight,
            256,
            256,
            color
        );
    }

    @Inject(
        method = "renderLabels",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$hideCraftingText(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        if (!HideCraftingText.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
