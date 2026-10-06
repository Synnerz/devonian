package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
    @Accessor("allMessages")
    List<GuiMessage> dv_getAllMessages();

    @Invoker("refreshTrimmedMessages")
    void dv_invokeRefresh();

    @Accessor("trimmedMessages")
    List<GuiMessage.Line> dv_getTrimmedMessages();

    @Invoker("getWidth")
    int dv_getWidth();

    @Invoker("getScale")
    double dv_getScale();
}
