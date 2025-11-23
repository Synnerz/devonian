package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("eyeHeightOld")
    float getEyeHeightOld();

    @Accessor("eyeHeightOld")
    void setEyeHeightOld(float f);

    @Accessor("eyeHeight")
    float getEyeHeight();

    @Accessor("eyeHeight")
    void setEyeHeight(float f);
}
