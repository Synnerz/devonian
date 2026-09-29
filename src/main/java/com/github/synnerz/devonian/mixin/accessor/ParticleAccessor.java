package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("friction")
    float dv_getFriction();

    @Accessor("friction")
    void dv_setFriction(float friction);

    @Accessor("speedUpWhenYMotionIsBlocked")
    boolean dv_getSpeedUpWhenYMotionIsBlocked();

    @Accessor("speedUpWhenYMotionIsBlocked")
    void dv_setSpeedUpWhenYMotionIsBlocked(boolean b);

    @Accessor("hasPhysics")
    boolean dv_getHasPhysics();

    @Accessor("hasPhysics")
    void dv_setHasPhysics(boolean b);
}
