package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.CustomContainerColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin extends AbstractContainerScreen<ChestMenu> {
    @Shadow @Final private static ResourceLocation CONTAINER_BACKGROUND;

    @Shadow @Final private int containerRows;

    public ContainerScreenMixin(ChestMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Inject(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V",
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            cancellable = true)
    private void devonian$renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci, int k, int l) {
        if (!CustomContainerColor.INSTANCE.isEnabled()) return;

        int color = CustomContainerColor.INSTANCE.getSETTING_CONTAINER_COLOR().get();

        ci.cancel();
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, k, l, 0.0F, 0.0F, imageWidth, containerRows * 18 + 17, 256, 256, color);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, k, l + containerRows * 18 + 17, 0.0F, 126.0F, imageWidth, 96, 256, 256, color);
    }
}
