package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ChangeCrouchHeight;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    @Inject(
        method = "tick",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$changeCrouchHeight(CallbackInfo ci) {
        if (!ChangeCrouchHeight.INSTANCE.isEnabled()) return;

        if (ChangeCrouchHeight.INSTANCE.tick((Camera) (Object) this)) ci.cancel();
    }
}
