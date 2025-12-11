package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableWorldLoadingScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(
        method = "notifyPlayerLoaded",
        at = @At("HEAD")
    )
    private void devonian$disableWorldLoadingScreen(CallbackInfo ci) {
        if (!DisableWorldLoadingScreen.INSTANCE.isEnabled()) return;
        DisableWorldLoadingScreen.INSTANCE.onPlayerLoaded();
    }
}
