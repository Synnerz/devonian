package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundGameEventPacket.Type.class)
public interface ClientboundGameEventPacket$TypeAccessor {
    @Accessor("id")
    int getId();
}
