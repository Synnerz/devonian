package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveRecipeBook;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
    @Shadow protected abstract void initButton();

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;initButton()V"))
    private void devonian$renderRecipeBook(AbstractRecipeBookScreen instance) {
        if (RemoveRecipeBook.INSTANCE.isEnabled()) return;
        this.initButton();
    }
}
