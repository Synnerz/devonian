package com.github.synnerz.devonian;

import net.minecraft.client.multiplayer.chat.GuiMessage;

import java.util.ListIterator;

public interface ChatComponentAccessor2 {
    GuiMessage devonian$getLastHoveredMessage();
    void devonian$setLastHoveredMessage(GuiMessage msg);

    void devonian$injectAddMessage(GuiMessage message, ListIterator<GuiMessage.Line> iterator);
}
