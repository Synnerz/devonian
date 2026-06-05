package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.ChatComponentAccessor2;
import com.github.synnerz.devonian.api.ChatUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$1")
public class ChatComponentInnerMixin {
    @Shadow
    @Final
    ChatComponent this$0;

    @WrapOperation(
        method = "accept",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z")
    )
    private boolean devonian$captureHoveredMessage(ChatComponent.ChatGraphicsAccess instance, int i, float v, FormattedCharSequence formattedCharSequence, Operation<Boolean> original, @Local(argsOnly = true) GuiMessage.Line line) {
        boolean hovered = original.call(instance, i, v, formattedCharSequence);

        if (hovered) ((ChatComponentAccessor2) this$0).devonian$setLastHoveredMessage(ChatUtils.INSTANCE.getMessageFromLine(line));

        return hovered;
    }
}
