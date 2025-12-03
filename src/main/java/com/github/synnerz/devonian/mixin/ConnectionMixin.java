package com.github.synnerz.devonian.mixin;

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
                    target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"
            ),
            cancellable = true
    )
    private void devonian$handlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (getReceiving() == PacketFlow.CLIENTBOUND) {
            if (new PacketReceivedEvent(packet).post()) ci.cancel();
        }
    }

    @Inject(
            method = "doSendPacket",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$sendPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean bl, CallbackInfo ci) {
        if (new PacketSentEvent(packet).post()) ci.cancel();
    }
}
