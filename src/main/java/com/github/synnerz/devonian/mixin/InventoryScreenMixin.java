package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.CustomContainerColor;
import com.github.synnerz.devonian.features.misc.HideCraftingText;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
    public InventoryScreenMixin(InventoryMenu recipeBookMenu, RecipeBookComponent<?> recipeBookComponent, Inventory inventory, Component component) {
        super(recipeBookMenu, recipeBookComponent, inventory, component);
    }

    @Redirect(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"
            )
    )
    private void devonian$renderBg(GuiGraphics instance, RenderPipeline renderPipeline, ResourceLocation resourceLocation, int i, int j, float f, float g, int k, int l, int m, int n) {
        int color = !CustomContainerColor.INSTANCE.isEnabled() ? -1 : CustomContainerColor.INSTANCE.getSETTING_CONTAINER_COLOR().get();

        instance.blit(
                renderPipeline,
                resourceLocation,
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
