package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.EventBus;
import com.github.synnerz.devonian.api.events.ParticleSpawnEvent;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(
            method = "add",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$addParticle(Particle particle, CallbackInfo ci) {
        EventBus.INSTANCE.post(new ParticleSpawnEvent(particle, ci));
    }
}
