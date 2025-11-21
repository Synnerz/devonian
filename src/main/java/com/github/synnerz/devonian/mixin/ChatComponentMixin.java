package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.CompactChat;
import com.github.synnerz.devonian.features.misc.RemoveChatLimit;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @Redirect(
            method = "addMessageToQueue",
            at = @At(value = "INVOKE", target = "Ljava/util/List;size()I")
    )
    private int devonian$onMessageSize(List instance) {
        if (RemoveChatLimit.INSTANCE.isEnabled() && instance.size() > 100) return 100;
        return instance.size();
    }

    @Redirect(
            method = "addMessageToDisplayQueue",
            at = @At(value = "INVOKE", target = "Ljava/util/List;size()I")
    )
    private int devonian$onMessageVisibleSize(List instance) {
        if (RemoveChatLimit.INSTANCE.isEnabled() && instance.size() > 100) return 100;
        return instance.size();
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component devonian$addMessage(Component text) {
        return CompactChat.INSTANCE.compactText(text);
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void devonian$clearMessages(boolean bl, CallbackInfo ci) {
        CompactChat.INSTANCE.clearHistory();
    }
}
