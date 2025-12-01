package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.EventBus;
import com.github.synnerz.devonian.api.events.PacketReceivedEvent;
import com.github.synnerz.devonian.api.events.PacketSentEvent;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow
    public abstract PacketFlow getReceiving();

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void devonian$handlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (getReceiving() == PacketFlow.CLIENTBOUND) {
            EventBus.INSTANCE.post(new PacketReceivedEvent(packet, ci));
        }
    }

    @Inject(
            method = "sendPacket",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$sendPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean bl, CallbackInfo ci) {
        EventBus.INSTANCE.post(new PacketSentEvent(packet, ci));
    }
}
