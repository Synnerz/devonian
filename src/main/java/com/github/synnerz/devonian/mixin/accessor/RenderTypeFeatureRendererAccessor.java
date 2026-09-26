package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(RenderTypeFeatureRenderer.class)
public interface RenderTypeFeatureRendererAccessor<Submit extends SubmitNode> {
    @Accessor("groups")
    List<RenderTypeFeatureRenderer.Group> getGroups();

    @Invoker("buildGroup")
    void invokeBuildGroup(FeatureFrameContext context, List<Submit> submits);

    @Accessor("currentGroup")
    void setCurrentGroup(RenderTypeFeatureRenderer.Group currentGroup);
}
