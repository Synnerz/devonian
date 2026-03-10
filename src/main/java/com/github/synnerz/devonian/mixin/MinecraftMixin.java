package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.Scheduler;
import com.github.synnerz.devonian.api.events.*;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Nullable
    public Screen screen;

    @Inject(
            method = "setScreen",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$setScreen(Screen screen, CallbackInfo ci) {
        if (screen == null && this.screen != null && new GuiCloseEvent(this.screen).post()) ci.cancel();
        if (screen != null && new GuiOpenEvent(screen).post()) ci.cancel();
    }

    @Inject(
        method = "startUseItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interactAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/EntityHitResult;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"),
        cancellable = true
    )
    private void devonian$entityInteract(CallbackInfo ci, @Local Entity entity) {
        EntityInteractEvent event = new EntityInteractEvent(entity);
        EventBus.INSTANCE.post(event);

        if (event.isCancelled()) ci.cancel();
    }

    @Inject(
            method = "run Tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/DeltaTracker$Timer;advanceTime(JZ)I"
            )
    )
    private void devonian$onRenderTick(boolean tick, CallbackInfo ci) {
        new RenderTickEvent().post();
    }

    @Inject(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketProcessor;processQueuedPackets()V")
    )
    private void devonian$schedulerBeforePacket(boolean bl, CallbackInfo ci) {
        Scheduler.INSTANCE.internalListenerBefore();
    }

    @Inject(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketProcessor;processQueuedPackets()V", shift = At.Shift.AFTER)
    )
    private void devonian$schedulerAfterPacket(boolean bl, CallbackInfo ci) {
        Scheduler.INSTANCE.internalListenerAfter();
    }
}
