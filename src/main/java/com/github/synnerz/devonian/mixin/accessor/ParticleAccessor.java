package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("friction")
    float getFriction();

    @Accessor("friction")
    void setFriction(float friction);

    @Accessor("speedUpWhenYMotionIsBlocked")
    boolean getSpeedUpWhenYMotionIsBlocked();

    @Accessor("speedUpWhenYMotionIsBlocked")
    void setSpeedUpWhenYMotionIsBlocked(boolean b);

    @Accessor("hasPhysics")
    boolean getHasPhysics();

    @Accessor("hasPhysics")
    void setHasPhysics(boolean b);
}
