package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.events.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(
            method = "setScreen",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$setScreen(Screen screen, CallbackInfo ci) {
        if (screen == null) {
            EventBus.INSTANCE.post(new GuiCloseEvent(ci));
            return;
        }

        EventBus.INSTANCE.post(new GuiOpenEvent(screen, ci));
    }

    @WrapOperation(
            method = "doItemUse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;")
    )
    private ActionResult devonian$blockInteract(ClientPlayerInteractionManager instance, ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, Operation<ActionResult> original) {
        ItemStack item = player.getStackInHand(hand);
        BlockPos pos = hitResult.getBlockPos();

        BlockInteractEvent event = new BlockInteractEvent(item, pos);
        EventBus.INSTANCE.post(event);

        if (!event.isCancelled()) return original.call(instance, player, hand, hitResult);

        return ActionResult.PASS;
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;beginRenderTick(JZ)I"
            )
    )
    private void devonian$onRenderTick(boolean tick, CallbackInfo ci) {
        new RenderTickEvent().post();
    }
}
