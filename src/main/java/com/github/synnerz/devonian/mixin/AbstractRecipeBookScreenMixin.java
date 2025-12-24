package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveRecipeBook;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
    @Inject(
        method = "init",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;initButton()V"),
        cancellable = true
    )
    private void devonian$renderRecipeBook(CallbackInfo ci) {
        if (!RemoveRecipeBook.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
