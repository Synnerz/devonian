package com.github.synnerz.devonian.mixin.accessor;

import com.mojang.blaze3d.opengl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlTexture.class)
public interface GlTextureAccessor {
    @Accessor("modesDirty")
    void setModesDirty(boolean dirty);
}
