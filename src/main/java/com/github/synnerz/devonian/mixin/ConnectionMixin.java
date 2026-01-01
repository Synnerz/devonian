package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devonian.api.events.PacketReceivedEvent;
import com.github.synnerz.devonian.api.events.PacketSentEvent;
import com.github.synnerz.devonian.api.events.PrePacketSentEvent;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.RunningOnDifferentThreadException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow
    public abstract PacketFlow getReceiving();

    @Shadow
    public abstract void disconnect(Component component);

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Nullable
    private volatile PacketListener packetListener;

    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"
        ),
        cancellable = true
    )
    private void devonian$handlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (getReceiving() != PacketFlow.CLIENTBOUND) return;

        boolean cancel = new PacketReceivedEvent(packet).post();

        if (packet instanceof ClientboundBundlePacket) {
            List<Packet<? super ClientGamePacketListener>> resend = new ArrayList<>();

            for (Packet<? super ClientGamePacketListener> subPacket : ((ClientboundBundlePacket) packet).subPackets()) {
                boolean c = new PacketReceivedEvent(subPacket).post();
                if (!c) resend.add(subPacket);
                cancel |= c;
            }

            if (cancel) {
                for (Packet<? super ClientGamePacketListener> packet1 : resend) {
                    try {
                        PacketUtils.ensureRunningOnSameThread(
                            packet1,
                            (ClientGamePacketListener) packetListener,
                            Devonian.INSTANCE.getMinecraft().packetProcessor()
                        );
                        packet1.handle((ClientGamePacketListener) packetListener);
                    } catch (RunningOnDifferentThreadException var5) {
                    } catch (RejectedExecutionException var6) {
                        this.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
                    } catch (ClassCastException classCastException) {
                        LOGGER.error("Received {} that couldn't be processed", packet.getClass(), classCastException);
                        this.disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
                    }
                }
            }
        }

        if (cancel) ci.cancel();
    }

    @Inject(
        method = "doSendPacket",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$sendPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean bl, CallbackInfo ci) {
        if (new PacketSentEvent(packet).post()) ci.cancel();
    }

    @Inject(
        method = "sendPacket",
        at = @At(value = "INVOKE", target = "Lio/netty/channel/EventLoop;execute(Ljava/lang/Runnable;)V"),
        cancellable = true
    )
    private void devonian$preSendPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean bl, CallbackInfo ci) {
        if (new PrePacketSentEvent(packet).post()) ci.cancel();
    }
}
