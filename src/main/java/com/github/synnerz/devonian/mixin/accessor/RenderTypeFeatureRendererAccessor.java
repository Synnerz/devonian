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
    List<RenderTypeFeatureRenderer.Group> dv_getGroups();

    @Invoker("buildGroup")
    void dv_buildGroup(FeatureFrameContext context, List<Submit> submits);

    @Accessor("currentGroup")
    void dv_setCurrentGroup(RenderTypeFeatureRenderer.Group currentGroup);
}
