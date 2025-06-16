package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.events.DropHandItemEvent;
import com.github.synnerz.devonian.events.EventBus;
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
        ItemStack stack = MinecraftClient.getInstance().player.getStackInHand(Hand.MAIN_HAND);
        if (stack != null && !stack.isEmpty()) {
            EventBus.INSTANCE.post(new DropHandItemEvent(stack, entireStack, cir));
        }
    }
}
