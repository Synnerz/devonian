package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("eyeHeightOld")
    float dv_getEyeHeightOld();

    @Accessor("eyeHeightOld")
    void dv_setEyeHeightOld(float f);

    @Accessor("eyeHeight")
    float dv_getEyeHeight();

    @Accessor("eyeHeight")
    void dv_setEyeHeight(float f);
}
