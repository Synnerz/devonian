package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.CustomContainerColor;
import com.github.synnerz.devonian.features.misc.HideCraftingText;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<@NotNull InventoryMenu> {
    public InventoryScreenMixin(InventoryMenu recipeBookMenu, RecipeBookComponent<?> recipeBookComponent, Inventory inventory, Component component) {
        super(recipeBookMenu, recipeBookComponent, inventory, component);
    }

    @WrapOperation(
        method = "extractBackground",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V")
    )
    private void devonian$renderBg(GuiGraphicsExtractor instance, RenderPipeline renderPipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, Operation<Void> original) {
        if (!CustomContainerColor.INSTANCE.isEnabled()) {
            original.call(instance, renderPipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
            return;
        }

        int color = CustomContainerColor.INSTANCE.getSETTING_CONTAINER_COLOR().get();
        instance.blit(
            renderPipeline,
            texture,
            x,
            y,
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
        method = "extractLabels",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$hideCraftingText(GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci) {
        if (!HideCraftingText.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
