package com.github.synnerz.devonian;

import net.minecraft.client.renderer.state.gui.GuiRenderState;

public interface GameRendererScaleAccessor {
    GuiRenderState devonian$guiRenderState();

    void devonian$setScaled(boolean scale);
    boolean devonian$getScaled();
}
