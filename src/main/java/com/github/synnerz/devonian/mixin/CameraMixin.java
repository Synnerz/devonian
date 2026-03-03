package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ChangeCrouchHeight;
import com.github.synnerz.devonian.features.misc.FixRidingCamera;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    @Unique
    private boolean cancelTick = false;

    @Inject(
        method = "tick",
        at = @At("HEAD")
    )
    private void devonian$changeCrouchHeight(CallbackInfo ci) {
        cancelTick = ChangeCrouchHeight.INSTANCE.tick((Camera) (Object) this);
    }

    @WrapWithCondition(
        method = "tick",
        at = {
            @At(value = "FIELD", target = "Lnet/minecraft/client/Camera;eyeHeightOld:F", opcode = Opcodes.PUTFIELD),
            @At(value = "FIELD", target = "Lnet/minecraft/client/Camera;eyeHeight:F", opcode = Opcodes.PUTFIELD),
        }
    )
    private boolean devonian$changeCrouchHeightOv(Camera instance, float value) {
        return !cancelTick;
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
