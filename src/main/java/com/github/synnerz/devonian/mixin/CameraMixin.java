package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ChangeCrouchHeight;
import com.github.synnerz.devonian.features.misc.FixRidingCamera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
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

    @WrapOperation(
        method = "setup",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F")
    )
    private float devonian$fixRidingCameraX(Entity instance, float f, Operation<Float> original) {
        if (!FixRidingCamera.INSTANCE.isEnabled() || !(instance instanceof LocalPlayer)) return original.call(instance, f);
        return instance.getXRot();
    }

    @WrapOperation(
        method = "setup",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F")
    )
    private float devonian$fixRidingCameraY(Entity instance, float f, Operation<Float> original) {
        if (!FixRidingCamera.INSTANCE.isEnabled() || !(instance instanceof LocalPlayer)) return original.call(instance, f);
        return instance.getYRot();
    }
}
