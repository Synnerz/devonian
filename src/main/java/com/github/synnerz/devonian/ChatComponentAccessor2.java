package com.github.synnerz.devonian;

import net.minecraft.client.GuiMessage;

public interface ChatComponentAccessor2 {
    GuiMessage devonian$getLastHoveredMessage();
    void devonian$setLastHoveredMessage(GuiMessage msg);
}
