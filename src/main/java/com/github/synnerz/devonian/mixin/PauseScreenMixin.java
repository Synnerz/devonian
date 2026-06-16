package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ConfirmDisconnect;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
    @Shadow @Nullable private Button disconnectButton;
    @Unique
    long lastClick = -1L;

    @WrapMethod(method = "lambda$createPauseMenu$7")
    private void devonian$onDisconnectButton(GridLayout.RowHelper helper, Holder dialogHolder, Operation<Void> original) {
        if (!ConfirmDisconnect.INSTANCE.isEnabled()) {
            original.call(helper, dialogHolder);
            return;
        }
        if (lastClick == -1L || System.currentTimeMillis() - lastClick < ConfirmDisconnect.INSTANCE.getSETTING_THRESHOLD().get()) {
            if (disconnectButton != null) disconnectButton.active = true;
            lastClick = System.currentTimeMillis();
            return;
        }

        original.call(helper, dialogHolder);
        lastClick = -1L;
    }
}
