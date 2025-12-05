package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.ChatUtils;
import com.github.synnerz.devonian.features.misc.CompactChat;
import com.github.synnerz.devonian.features.misc.DisableChatAutoScroll;
import com.github.synnerz.devonian.features.misc.RemoveChatLimit;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Redirect(
            method = "addMessageToQueue",
            at = @At(value = "INVOKE", target = "Ljava/util/List;size()I")
    )
    private int devonian$onMessageSize(List<?> instance) {
        if (RemoveChatLimit.INSTANCE.isEnabled() && instance.size() > 100) return 100;
        return instance.size();
    }

    @Redirect(
            method = "addMessageToDisplayQueue",
            at = @At(value = "INVOKE", target = "Ljava/util/List;size()I")
    )
    private int devonian$onMessageVisibleSize(List<?> instance) {
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

    @Redirect(
            method = "addMessageToDisplayQueue",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;scrollChat(I)V")
    )
    private void devonian$onChatScroll(ChatComponent instance, int i) {
        if (DisableChatAutoScroll.INSTANCE.isEnabled()) return;
        instance.scrollChat(i);
    }

    @Inject(
        method = "addMessageToDisplayQueue",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", shift = At.Shift.AFTER)
    )
    private void devonian$trackDisplayLine(GuiMessage guiMessage, CallbackInfo ci) {
        ChatUtils.INSTANCE.getLineCache().put(this.trimmedMessages.getFirst(), guiMessage);
    }

    @Inject(
        method = "refreshTrimmedMessages",
        at = @At("HEAD")
    )
    private void devonian$refreshTrimmedMessages(CallbackInfo ci) {
        ChatUtils.INSTANCE.getLineCache().clear();
    }
}
