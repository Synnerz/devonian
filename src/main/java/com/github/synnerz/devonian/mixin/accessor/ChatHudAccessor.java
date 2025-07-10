package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatHud.class)
public interface ChatHudAccessor {
    @Accessor
    List<ChatHudLine> getMessages();

    @Accessor
    List<ChatHudLine.Visible> getVisibleMessages();

    @Invoker("toChatLineX")
    double toChatLineMX(double x);

    @Invoker("toChatLineY")
    double toChatLineMY(double y);

    @Invoker("getMessageLineIndex")
    int getMessageLineIdx(double chatLineX, double chatLineY);

    @Invoker
    void invokeRefresh();
}
