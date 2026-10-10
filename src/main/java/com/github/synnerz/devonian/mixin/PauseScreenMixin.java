package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ConfirmDisconnect;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
    @Shadow @Nullable private Button disconnectButton;
    @Unique
    long lastClick = -1L;

    @WrapMethod(method = "lambda$createPauseMenu$10")
    private void devonian$onDisconnectButton(Button button, Operation<Void> original) {
        if (!ConfirmDisconnect.INSTANCE.isEnabled()) {
            original.call(button);
            return;
        }
        if (lastClick == -1L || System.currentTimeMillis() - lastClick < ConfirmDisconnect.INSTANCE.getSETTING_THRESHOLD().get()) {
            if (disconnectButton != null) disconnectButton.active = true;
            lastClick = System.currentTimeMillis();
            return;
        }

        original.call(button);
        lastClick = -1L;
    }
}
