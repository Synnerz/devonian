package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.CustomContainerColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin extends AbstractContainerScreen<@NotNull ChestMenu> {
    @Shadow @Final private static Identifier CONTAINER_BACKGROUND;

    @Shadow @Final private int containerRows;

    public ContainerScreenMixin(ChestMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Inject(
            method = "extractBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V",
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            cancellable = true)
    private void devonian$renderBg(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci, int xo, int yo) {
        if (!CustomContainerColor.INSTANCE.isEnabled()) return;

        int color = CustomContainerColor.INSTANCE.getSETTING_CONTAINER_COLOR().get();

        ci.cancel();
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, xo, yo, 0.0F, 0.0F, imageWidth, containerRows * 18 + 17, 256, 256, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, xo, yo + containerRows * 18 + 17, 0.0F, 126.0F, imageWidth, 96, 256, 256, color);
    }
}
