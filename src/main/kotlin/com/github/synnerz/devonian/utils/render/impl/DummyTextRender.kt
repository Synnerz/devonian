package com.github.synnerz.devonian.utils.render.impl

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.feature.FeatureFrameContext
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer
import net.minecraft.client.renderer.feature.TextFeatureRenderer
import net.minecraft.client.renderer.rendertype.RenderType

// TODO: find a better way of doing this (since it exists) whenever not feeling lazy af
object DummyTextRender : RenderTypeFeatureRenderer<TextFeatureRenderer.Submit>() {
    override fun buildGroup(
        context: FeatureFrameContext,
        submits: List<TextFeatureRenderer.Submit>
    ) {}

    fun findBuffer(renderType: RenderType): VertexConsumer {
        return this.getVertexBuilder(renderType)
    }
}