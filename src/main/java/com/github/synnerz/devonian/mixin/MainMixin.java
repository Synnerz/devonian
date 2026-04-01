package com.github.synnerz.devonian.mixin;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {
    // Using same approach as https://github.com/comp500/ScreenshotToClipboard/blob/1.20-arch/common/src/main/java/link/infra/screenshotclipboard/common/mixin/AWTHackMixin.java#L12
    // Licensed under MIT license
    // since headless gets set somewhere in the process, and we need it for AutoCopyScreenshot
    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void devonian$onMinecraftMain(String[] strings, CallbackInfo ci) {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) return;
        System.setProperty("java.awt.headless", "false");
    }
}
