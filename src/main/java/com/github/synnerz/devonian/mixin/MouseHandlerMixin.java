package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.KeyShortcuts;
import com.github.synnerz.devonian.features.misc.inventory.NoCursorReset;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
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

    @Shadow
    private boolean ignoreFirstMove;

    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @WrapWithCondition(
        method = "releaseMouse",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MouseHandler;xpos:D",
            opcode = Opcodes.PUTFIELD
        )
    )
    private boolean devonian$setXCursorRelease(MouseHandler instance, double value) {
        return NoCursorReset.INSTANCE.shouldReset();
    }

    @WrapWithCondition(
        method = "releaseMouse",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MouseHandler;ypos:D",
            opcode = Opcodes.PUTFIELD
        )
    )
    private boolean devonian$setYCursorRelease(MouseHandler instance, double value) {
        return NoCursorReset.INSTANCE.shouldReset();
    }

    @Inject(
        method = "releaseMouse",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(Lcom/mojang/blaze3d/platform/Window;IDD)V", shift = At.Shift.AFTER)
    )
    private void devonian$releaseMouseSetPosFix(CallbackInfo ci) {
        if (!NoCursorReset.INSTANCE.isEnabled()) return;
        GLFW.glfwSetCursorPos(minecraft.getWindow().handle(), xpos, ypos);
        this.ignoreFirstMove = true;
    }

    @WrapWithCondition(
        method = "grabMouse",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MouseHandler;xpos:D",
            opcode = Opcodes.PUTFIELD
        )
    )
    private boolean devonian$setXCursorGrab(MouseHandler instance, double value) {
        return !NoCursorReset.INSTANCE.isEnabled();
    }

    @WrapWithCondition(
        method = "grabMouse",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MouseHandler;ypos:D",
            opcode = Opcodes.PUTFIELD
        )
    )
    private boolean devonian$setYCursorGrab(MouseHandler instance, double value) {
        return !NoCursorReset.INSTANCE.isEnabled();
    }

    @Inject(method = "onButton", at = @At("TAIL"))
    private void devonian$onButton(long l, MouseButtonInfo mouseButtonInfo, int i, CallbackInfo ci) {
        if (l != minecraft.getWindow().handle()) return;
        if (minecraft.screen != null) return;

        KeyShortcuts.INSTANCE.onButtonPress(mouseButtonInfo);
    }
}
