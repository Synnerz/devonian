package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.ClientBlockUpdateEvent;
import com.github.synnerz.devonian.api.events.ClientSoundPlayEvent;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level implements CacheSlot.Cleaner<ClientLevel> {
    protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }

    @Inject(
            method = "playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onClientSound(double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource, float g, float h, boolean bl, CallbackInfo ci) {
        var soundName = soundEvent.location();
        if (new ClientSoundPlayEvent(
                soundName.getNamespace() + ":" + soundName.getPath(),
                h,
                g,
                soundSource,
                d, e, f,
                soundEvent
        ).post()) ci.cancel();
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("HEAD"))
    private void devonian$onClientBlockUpdate(BlockPos blockPos, BlockState blockState, int i, CallbackInfo ci) {
        new ClientBlockUpdateEvent(getBlockState(blockPos), blockState, blockPos).post();
    }
}
