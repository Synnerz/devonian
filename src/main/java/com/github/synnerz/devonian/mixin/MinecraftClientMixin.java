package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.events.Events;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(
            method = "setScreen",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$setScreen(Screen screen, CallbackInfo ci) {
        if (screen == null) {
            Events.GUI_CLOSE.invoker().trigger(null, ci);
            return;
        }

        Events.GUI_OPEN.invoker().trigger(screen, ci);
    }
}
