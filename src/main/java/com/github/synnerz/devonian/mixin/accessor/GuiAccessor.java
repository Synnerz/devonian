package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface GuiAccessor {
    @Accessor("random")
    RandomSource getRandom();

    @Invoker("renderHeart")
    void invokeRenderHeart(
        GuiGraphics guiGraphics, Gui.HeartType heartType,
        int x, int y,
        boolean hardcore, boolean blinking, boolean half
    );
}
