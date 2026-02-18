package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.EventBus;
import com.github.synnerz.devonian.utils.render.ChromaText;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlobalSettingsUniform.class)
public class GlobalSettingsUniformMixin {
    @Inject(
        method = "update",
        at = @At("TAIL")
    )
    private void devonian$chromaText(int i, int j, double d, long l, DeltaTracker deltaTracker, int k, Camera camera, boolean bl, CallbackInfo ci) {
        ChromaText.INSTANCE.updateBuffer(EventBus.INSTANCE.getClientTicks() + deltaTracker.getGameTimeDeltaPartialTick(true));
    }
}
