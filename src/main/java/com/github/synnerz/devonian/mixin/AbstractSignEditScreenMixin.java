package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.SignEnterKey;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen {
    @Shadow private SignText text;

    protected AbstractSignEditScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void devonian$onSignEnter(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!keyEvent.isConfirmation()) return;
        if (!SignEnterKey.INSTANCE.shouldEnter(List.of(text.getMessages(false)))) return;

        minecraft.setScreen(null);
        cir.setReturnValue(true);
        cir.cancel();
    }
}
