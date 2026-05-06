package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Hud.class)
public interface HudAccessor {
    @Accessor("random")
    RandomSource getRandom();

    @Invoker("extractHeart")
    void invokeRenderHeart(
            GuiGraphicsExtractor guiGraphics, Hud.HeartType heartType,
            int x, int y,
            boolean hardcore, boolean blinking, boolean half
    );
}
