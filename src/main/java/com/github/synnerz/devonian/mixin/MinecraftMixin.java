package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.Scheduler;
import com.github.synnerz.devonian.api.events.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Final
    public Gui gui;

    @Shadow
    private static Minecraft instance;

    @Inject(
            method = "setScreenAndShow",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$setScreen(Screen screen, CallbackInfo ci) {
        if (screen == null && this.gui.screen() != null && new GuiCloseEvent(this.gui.screen()).post()) ci.cancel();
        if (screen != null && new GuiOpenEvent(screen).post()) ci.cancel();
    }

    @Inject(
        method = "startUseItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/EntityHitResult;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"),
        cancellable = true
    )
    private void devonian$entityInteract(CallbackInfo ci, @Local Entity entity) {
        EntityInteractEvent event = new EntityInteractEvent(entity);
        EventBus.INSTANCE.post(event);

        if (event.isCancelled()) ci.cancel();
    }

    @Inject(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/DeltaTracker$Timer;advanceGameTime(J)I"
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

    @WrapOperation(
            method = "startUseItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;")
    )
    private InteractionResult devonian$blockInteract(MultiPlayerGameMode instance, LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, Operation<InteractionResult> original) {
        ItemStack item = localPlayer.getItemInHand(interactionHand);
        BlockPos pos = blockHitResult.getBlockPos();

        if (!new ClientBlockInteractEvent(item, pos).post()) return original.call(instance, localPlayer, interactionHand, blockHitResult);

        return InteractionResult.PASS;
    }

    @Inject(method = "disconnectFromWorld", at = @At("HEAD"))
    private void devonian$onClientDestroy(CallbackInfo ci) {
        new WorldDestroyEvent(instance).post();
    }
}
