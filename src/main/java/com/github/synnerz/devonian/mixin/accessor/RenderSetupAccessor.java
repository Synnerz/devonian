package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {
    @Accessor("textures")
    Map<String, RenderSetup.TextureBinding> getTextures2();

    @Accessor("textureTransform")
    TextureTransform getTextureTransform();

    @Accessor("outputTarget")
    OutputTarget getOutputTarget();
}
