package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent;
import com.github.synnerz.devonian.api.events.EventBus;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerImplMixin {
    @Inject(
        method = "handlePing",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER)
    )
    private void devonian$clientThreadServerTick(ClientboundPingPacket clientboundPingPacket, CallbackInfo ci) {
        if (EventBus.INSTANCE.get_internalSkipPing().remove(clientboundPingPacket.getId())) return;
        if (clientboundPingPacket.getId() < 0) new ClientThreadServerTickEvent().post();
    }
}
