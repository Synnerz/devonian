package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.KeyPressEvent;
import com.github.synnerz.devonian.api.events.KeyReleaseEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @WrapMethod(method = "keyPress")
    private void devonian$onKeyPress(long l, int i, KeyEvent keyEvent, Operation<Void> original) {
        original.call(l, i, keyEvent);

        if (l != minecraft.getWindow().handle()) return;
        if (minecraft.gui.screen() != null || minecraft.level == null) return;

        switch (i) {
            case GLFW.GLFW_RELEASE:
                new KeyReleaseEvent(keyEvent).post();
                break;

            case GLFW.GLFW_PRESS:
                new KeyPressEvent(keyEvent).post();
                break;
        }
    }
}
