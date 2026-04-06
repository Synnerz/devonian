package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.EventBus;
import com.github.synnerz.devonian.utils.render.ChromaText;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.world.phys.Vec3;
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
    private void devonian$chromaText(int width, int height, double glintAlpha, long gameTime, DeltaTracker deltaTracker, int menuBlurRadius, Vec3 cameraPos, boolean useRgss, CallbackInfo ci) {
        ChromaText.INSTANCE.updateBuffer(EventBus.INSTANCE.getClientTicks() + deltaTracker.getGameTimeDeltaPartialTick(true));
    }
}
