package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.EntityDeathEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(
            method = "die",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setPose(Lnet/minecraft/world/entity/Pose;)V"
            )
    )
    private void devonian$onDeath(DamageSource damageSource, CallbackInfo ci) {
        var world = this.level();
        if (!world.isClientSide) return;

        new EntityDeathEvent(this, (ClientLevel) world).post();
    }
}
