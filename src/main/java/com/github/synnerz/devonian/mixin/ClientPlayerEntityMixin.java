package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.events.Events;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void devonian$dropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        assert MinecraftClient.getInstance().player != null;
        ItemStack stack = MinecraftClient.getInstance().player.getStackInHand(Hand.MAIN_HAND);
        if (stack != null && !stack.isEmpty()) {
            Events.DROP_HAND_ITEM.invoker().onDrop(stack, entireStack, cir);
        }
    }
}
