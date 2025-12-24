package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {
    @Invoker("insertText")
    void insertText(String text, boolean overwrite);
}
