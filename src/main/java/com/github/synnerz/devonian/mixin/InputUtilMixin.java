package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.NoCursorReset;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InputUtil.class)
public class InputUtilMixin {
    @Redirect(
            method = "setCursorParameters",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetCursorPos(JDD)V", remap = false)
    )
    private static void devonian$setCursorPos(long window, double xpos, double ypos) {
        // TODO: find better injection point
        if (NoCursorReset.INSTANCE.shouldReset()) {
            GLFW.glfwSetCursorPos(window, xpos, ypos);
        }
    }
}
