package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {
    @Accessor("xLast")
    double dv_getLastXClient();

    @Accessor("yLast")
    double dv_getLastYClient();

    @Accessor("zLast")
    double dv_getLastZClient();

    @Accessor("yRotLast")
    float dv_getLastYawClient();

    @Accessor("xRotLast")
    float dv_getLastPitchClient();
}
