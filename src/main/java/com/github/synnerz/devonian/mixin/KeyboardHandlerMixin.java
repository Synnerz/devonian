package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.KeyShortcuts;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("TAIL"))
    private void devonian$onKeyPress(long l, int i, KeyEvent keyEvent, CallbackInfo ci) {
        Window window = minecraft.getWindow();
        Screen screen = minecraft.screen;
        if (screen != null || minecraft.level == null) return;
        if (l != window.handle()) return;

        KeyShortcuts.INSTANCE.onKeyPress(keyEvent);
    }
}
