package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.inventory.NoCursorReset;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GLFW.class)
public class GLFWMixin {
    @Inject(
        method = "glfwPollEvents",
        at = @At("TAIL"),
        remap = false
    )
    private static void devonian$noCursorReset(CallbackInfo ci) {
        NoCursorReset.ignoreFirstBatch--;
    }
}
