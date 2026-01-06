package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.KeyPressEvent;
import com.github.synnerz.devonian.features.misc.KeyShortcuts;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void devonian$onKeyPress(long l, int i, KeyEvent keyEvent, CallbackInfo ci, Window window) {
        if (l != window.handle()) return;
        if (minecraft.screen != null || minecraft.level == null) return;
        if (i != GLFW.GLFW_PRESS) return;

        new KeyPressEvent(keyEvent.key(), keyEvent.scancode(), keyEvent).post();
        KeyShortcuts.INSTANCE.onKeyPress(keyEvent);
    }
}
