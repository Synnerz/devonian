package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    @Accessor("guiSprites")
    TextureAtlas getGuiSprites();

    @Invoker("getSpriteScaling")
    GuiSpriteScaling invokeSpriteScaling(TextureAtlasSprite sprite);
}
