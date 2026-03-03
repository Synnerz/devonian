package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.MouseHandlerAccessor;
import com.github.synnerz.devonian.api.events.MousePressEvent;
import com.github.synnerz.devonian.api.events.MouseReleaseEvent;
import com.github.synnerz.devonian.api.events.MouseScrollEvent;
import com.github.synnerz.devonian.features.debug.MousePositionLogger;
import com.github.synnerz.devonian.features.misc.inventory.NoCursorReset;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin implements MouseHandlerAccessor {
    @Shadow @Final private Minecraft minecraft;

    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @Shadow
    private boolean ignoreFirstMove;

    @Shadow
    public abstract double getScaledXPos(Window window);

    @Shadow
    public abstract double getScaledYPos(Window window);

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

    @WrapOperation(
        method = "releaseMouse",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(Lcom/mojang/blaze3d/platform/Window;IDD)V")
    )
    private void devonian$releaseMouseSetPosFix(Window window, int i, double d, double e, Operation<Void> original) {
        if (!NoCursorReset.INSTANCE.isEnabled()) {
            original.call(window, i, d, e);
            return;
        }
        GLFW.glfwSetInputMode(window.handle(), 208897, i);
        // GLFW.glfwSetCursorPos(window.handle(), xpos, ypos);
        NoCursorReset.ignoreFirstBatch = 3;
        NoCursorReset.setCursorPos = true;
        NoCursorReset.cursorPosX = xpos;
        NoCursorReset.cursorPosY = ypos;
        // ignoreFirstMove = true;
    }

    @Redirect(
        method = "onMove",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/MouseHandler;ignoreFirstMove:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean devonian$releaseMouseSetPosFixIgnore(MouseHandler instance) {
        return NoCursorReset.ignoreFirstBatch > 0 || ignoreFirstMove;
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

    @WrapMethod(method = "onButton")
    private void devonian$onButton(long l, MouseButtonInfo mouseButtonInfo, int i, Operation<Void> original) {
        original.call(l, mouseButtonInfo, i);

        Window w = minecraft.getWindow();
        if (l != w.handle()) return;
        if (minecraft.screen != null || minecraft.level == null) return;

        double x = getScaledXPos(w);
        double y = getScaledYPos(w);
        switch (i) {
            case GLFW.GLFW_RELEASE:
                new MouseReleaseEvent(x, y, mouseButtonInfo).post();
                break;

            case GLFW.GLFW_PRESS:
                new MousePressEvent(x, y, mouseButtonInfo).post();
                break;
        }
    }

    @Inject(
        method = "onMove",
        at = @At("TAIL")
    )
    private void devonian$mouseLoggerMove(long l, double d, double e, CallbackInfo ci) {
        MousePositionLogger.INSTANCE.onMove(l, d, e, ignoreFirstMove, xpos, ypos);
    }

    @Inject(
        method = "grabMouse",
        at = @At("HEAD")
    )
    private void devonian$mouseLoggerGrab(CallbackInfo ci) {
        MousePositionLogger.INSTANCE.onGrab();
    }

    @Inject(
        method = "releaseMouse",
        at = @At("HEAD")
    )
    private void devonian$mouseLoggerRelease(CallbackInfo ci) {
        MousePositionLogger.INSTANCE.onRelease();
    }

    @Unique
    private static int guiScaledWidthOverride = -1;
    @Unique
    private static int guiScaledHeightOverride = -1;

    @Override
    public void devonian$setGuiScaledWidthOverride(int w) {
        guiScaledWidthOverride = w;
    }

    @Override
    public void devonian$setGuiScaledHeightOverride(int h) {
        guiScaledHeightOverride = h;
    }

    @WrapOperation(
        method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;D)D",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I")
    )
    private static int devonian$guiScaleEventX(Window instance, Operation<Integer> original) {
        if (guiScaledWidthOverride != -1) return guiScaledWidthOverride;
        return original.call(instance);
    }

    @WrapOperation(
        method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;D)D",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I")
    )
    private static int devonian$guiScaleEventY(Window instance, Operation<Integer> original) {
        if (guiScaledHeightOverride != -1) return guiScaledHeightOverride;
        return original.call(instance);
    }

    @Inject(
            method = "onScroll",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/ScrollWheelHandler;onMouseScroll(DD)Lorg/joml/Vector2i;"
            ),
            cancellable = true
    )
    private void devonian$onScroll(long l, double d, double e, CallbackInfo ci) {
        if (new MouseScrollEvent(e).post()) ci.cancel();
    }
}
