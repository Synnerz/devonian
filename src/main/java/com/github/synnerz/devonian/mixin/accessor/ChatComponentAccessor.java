package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
    @Accessor("allMessages")
    List<GuiMessage> getMessages();

    @Accessor("trimmedMessages")
    List<GuiMessage.Line> getVisibleMessages();

    @Invoker("screenToChatX")
    double toChatLineMX(double x);

    @Invoker("screenToChatY")
    double toChatLineMY(double y);

    @Invoker("getMessageLineIndexAt")
    int getMessageLineIdx(double chatLineX, double chatLineY);

    @Invoker("refreshTrimmedMessages")
    void invokeRefresh();
}
