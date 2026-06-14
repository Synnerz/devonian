package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.FixBowPull;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class PlayerMixin {
    @WrapOperation(method = "getProjectile", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack devonian$onGetProjectile(Inventory instance, int i, Operation<ItemStack> original) {
        if (FixBowPull.INSTANCE.shouldFix()) return ItemStack.EMPTY;
        return original.call(instance, i);
    }
}
