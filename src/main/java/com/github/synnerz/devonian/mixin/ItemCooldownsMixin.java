package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableEnderPearlCooldown;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCooldowns.class)
public class ItemCooldownsMixin {
    @Inject(
        method = "getCooldownPercent",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$disableEnderPearlCooldown(ItemStack itemStack, float f, CallbackInfoReturnable<Float> cir) {
        if (!DisableEnderPearlCooldown.INSTANCE.isEnabled()) return;
        if (itemStack.getItem() == Items.ENDER_PEARL) cir.setReturnValue(0f);
    }
}
