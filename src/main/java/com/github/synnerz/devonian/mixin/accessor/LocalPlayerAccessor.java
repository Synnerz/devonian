package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {
    @Accessor("xLast")
    double getLastXClient();

    @Accessor("yLast")
    double getLastYClient();

    @Accessor("zLast")
    double getLastZClient();

    @Accessor("yRotLast")
    float getLastYawClient();

    @Accessor("xRotLast")
    float getLastPitchClient();
}
