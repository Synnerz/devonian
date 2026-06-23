package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.TooltipRenderEvent;
import com.github.synnerz.devonian.features.misc.inventory.ScrollableTooltip;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2ic;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsMixin {
    @Shadow public abstract Matrix3x2fStack pose();
    @Unique private boolean devonian$pushed = false;

    @WrapOperation(
            method = "tooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"
            )
    )
    private Vector2ic devonian$onToolTipRender(ClientTooltipPositioner instance, int width, int height, int i, int j, int k, int l, Operation<Vector2ic> original) {
        if (!ScrollableTooltip.INSTANCE.isEnabled()) {
            return original.call(instance, width, height, i, j, k, l);
        }

        float scale = (float) ScrollableTooltip.INSTANCE.scale();
        double xoffset = ScrollableTooltip.INSTANCE.xoffset();
        double yoffset = ScrollableTooltip.INSTANCE.yoffset();
        boolean lockInPlace = ScrollableTooltip.INSTANCE.getSETTING_LOCK_IN_PLACE().get();

        pose().pushMatrix();
        devonian$pushed = true;
        // Scale (zoom in)
        if (scale != 0f)
            pose().scale(scale, scale);
        else
            scale = 1f;

        // Translate to offset
        if ((xoffset != 0d || yoffset != 0d) && !lockInPlace)
            pose().translate((float) xoffset, (float) yoffset);

        if (lockInPlace) {
            if (ScrollableTooltip.INSTANCE.getSETTING_DONT_DIVIDE_BY_SCALE().get())
                pose().translate((float) xoffset, (float) yoffset);
            else
                pose().translate((float) xoffset / scale, (float) yoffset / scale);
            return original.call(instance, width, height, 0, 0, k, l);
        }

        var vec = original.call(instance, width, height, (int) (i / scale), (int) (j / scale), k, l);

        ScrollableTooltip.INSTANCE.onRender(i, j, k, l);

        // TODO: fix the y value not being adjusted properly
        return vec;
    }

    @Inject(method = "tooltip", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;popMatrix()Lorg/joml/Matrix3x2fStack;", remap = false))
    private void devonian$onPostRenderTooltip(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner, Identifier resourceLocation, CallbackInfo ci) {
        if (devonian$pushed) {
            pose().popMatrix();
            devonian$pushed = false;
        }
    }

    @Inject(method = "setTooltipForNextFrameInternal", at = @At("HEAD"), cancellable = true)
    private void devonian$renderTooltip(Font font, List<ClientTooltipComponent> lines, int xo, int yo, ClientTooltipPositioner positioner, @Nullable Identifier style, boolean replaceExisting, CallbackInfo ci) {
        if (new TooltipRenderEvent(lines instanceof ArrayList<?> ? lines : new ArrayList<>(lines), xo, yo).post()) ci.cancel();
    }
}
