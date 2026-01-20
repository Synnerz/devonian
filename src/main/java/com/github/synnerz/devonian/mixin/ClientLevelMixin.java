package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.ClientSoundPlayeEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(
            method = "playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onClientSound(double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource, float g, float h, boolean bl, CallbackInfo ci) {
        var soundName = soundEvent.location();
        if (new ClientSoundPlayeEvent(
                soundName.getNamespace() + ":" + soundName.getPath(),
                h,
                g,
                soundSource,
                d, e, f,
                soundEvent
        ).post()) ci.cancel();
    }
}
