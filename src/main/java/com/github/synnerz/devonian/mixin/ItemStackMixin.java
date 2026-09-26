package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.ItemUtils;
import com.github.synnerz.devonian.features.misc.CustomizeItems;
import com.github.synnerz.devonian.features.misc.inventory.OldMasterStar;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @WrapOperation(
        method = "getCustomName",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;", ordinal = 0)
    )
    private Object devonian$oldMasterStar(ItemStack instance, DataComponentType dataComponentType, Operation<Object> original) {
        Object orig = original.call(instance, dataComponentType);
        if (!(orig instanceof Component c)) return orig;

        if (CustomizeItems.INSTANCE.isEnabled()) {
            String uuid = ItemUtils.INSTANCE.uuid(instance);
            if (uuid != null) c = CustomizeItems.INSTANCE.getNameComponents().getOrDefault(uuid, c);
        }
        if (OldMasterStar.INSTANCE.isEnabled()) c = OldMasterStar.INSTANCE.transformName(c);

        return c;
    }
}
