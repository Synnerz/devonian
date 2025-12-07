package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.inventory.NoCursorReset;
import com.github.synnerz.devonian.features.misc.KeyShortcuts;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @WrapWithCondition(
            method = {"releaseMouse", "grabMouse"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/MouseHandler;xpos:D",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private boolean devonian$setXCursor(MouseHandler instance, double value) {
        return NoCursorReset.INSTANCE.shouldReset();
    }

    @WrapWithCondition(
            method = {"releaseMouse", "grabMouse"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/MouseHandler;ypos:D",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private boolean devonian$setYCursor(MouseHandler instance, double value) {
        return NoCursorReset.INSTANCE.shouldReset();
    }

    @Inject(
            method = "onButton",
            at = @At("TAIL")
    )
    private void devonian$onButton(long l, MouseButtonInfo mouseButtonInfo, int i, CallbackInfo ci) {
        if (l != minecraft.getWindow().handle()) return;
        if (minecraft.screen != null) return;

        KeyShortcuts.INSTANCE.onButtonPress(mouseButtonInfo);
    }
}
