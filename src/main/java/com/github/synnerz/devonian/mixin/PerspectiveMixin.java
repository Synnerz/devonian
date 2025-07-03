package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveFrontView;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Perspective.class)
public class PerspectiveMixin {
    @Shadow @Final private boolean firstPerson;

    @Inject(
            method = "next",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$onPerspectiveChange(CallbackInfoReturnable<Perspective> cir) {
        if (!RemoveFrontView.INSTANCE.isEnabled()) return;
        cir.setReturnValue(this.firstPerson ? Perspective.THIRD_PERSON_BACK : Perspective.FIRST_PERSON);
    }
}
