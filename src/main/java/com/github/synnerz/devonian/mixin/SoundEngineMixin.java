package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.utils.CustomSounds;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Options;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Shadow
    @Final
    private Options options;

    @WrapMethod(method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F")
    private float devonian$onSoundEnginePlay(float f, SoundSource soundSource, Operation<Float> original) {
        var que = CustomSounds.INSTANCE.getQueued();
        if (que != null) {
            return Mth.clamp(que.volume, 0.0f, 10.0f) * Mth.clamp(this.options.getFinalSoundSourceVolume(soundSource), 0.0F, 10.0F);
        }
        return original.call(f, soundSource);
    }
}
