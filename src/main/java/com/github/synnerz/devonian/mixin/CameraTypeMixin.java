package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveFrontView;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CameraType.class)
public class CameraTypeMixin {
    @Shadow @Final private boolean firstPerson;

    @Inject(
            method = "cycle",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onPerspectiveChange(CallbackInfoReturnable<CameraType> cir) {
        if (!RemoveFrontView.INSTANCE.isEnabled()) return;
        cir.setReturnValue(this.firstPerson ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
    }
}
