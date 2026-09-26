package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.ItemUtils;
import com.github.synnerz.devonian.features.misc.CustomizeItems;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataComponentHolder.class)
public interface DataComponentHolderMixin {
    @Inject(
        method = "get",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$customizeItems(DataComponentType<?> type, CallbackInfoReturnable<Object> cir) {
        if (type != DataComponents.ITEM_MODEL) return;
        DataComponentHolder that = (DataComponentHolder) this;
        // ij lies
        if (that.getClass() != ItemStack.class) return;
        if (!CustomizeItems.INSTANCE.isEnabled()) return;

        ItemStack stack = (ItemStack) (Object) this;
        String id = ItemUtils.INSTANCE.uuid(stack);
        if (id == null) return;
        Identifier modelId = CustomizeItems.INSTANCE.getItemModels().get(id);

        if (modelId != null) cir.setReturnValue(modelId);
    }
}
