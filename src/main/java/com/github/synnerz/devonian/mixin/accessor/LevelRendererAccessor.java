package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    @Accessor("renderBuffers")
    RenderBuffers getRenderBuffers();

    @Accessor
    SubmitNodeStorage getSubmitNodeStorage();
}
