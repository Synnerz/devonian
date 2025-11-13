package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor {
    @Accessor("lastXClient")
    double getLastXClient();

    @Accessor("lastYClient")
    double getLastYClient();

    @Accessor("lastZClient")
    double getLastZClient();

    @Accessor("lastYawClient")
    float getLastYawClient();

    @Accessor("lastPitchClient")
    float getLastPitchClient();
}
