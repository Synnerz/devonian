package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.ChatComponentAccessor2;
import com.github.synnerz.devonian.api.ChatUtils;
import com.github.synnerz.devonian.features.misc.DisableChatAutoScroll;
import com.github.synnerz.devonian.features.misc.PeekChatKeybind;
import com.github.synnerz.devonian.features.misc.RemoveChatLimit;
import com.github.synnerz.devonian.features.misc.chat.CompactChat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedList;
import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin implements ChatComponentAccessor2 {
    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @ModifyConstant(
        method = "addMessageToDisplayQueue",
        constant = @Constant(intValue = 100)
    )
    private int devonian$removeChatLimitaddMessageToDisplayQueue(int constant) {
        if (!RemoveChatLimit.INSTANCE.isEnabled()) return constant;
        return RemoveChatLimit.INSTANCE.getSETTING_MAX_MESSAGES().get().intValue();
    }

    @ModifyConstant(
        method = "addMessageToQueue",
        constant = @Constant(intValue = 100)
    )
    private int devonian$removeChatLimitaddMessageToQueue(int constant) {
        if (!RemoveChatLimit.INSTANCE.isEnabled()) return constant;
        return RemoveChatLimit.INSTANCE.getSETTING_MAX_MESSAGES().get().intValue();
    }

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true)
    private Component devonian$addMessage(Component text) {
        return CompactChat.INSTANCE.compactText(text);
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void devonian$clearMessages(boolean bl, CallbackInfo ci) {
        CompactChat.INSTANCE.clearHistory();
    }

    @WrapOperation(
        method = "addMessageToDisplayQueue",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;scrollChat(I)V")
    )
    private void devonian$onChatScroll(ChatComponent instance, int i, Operation<Void> original) {
        if (DisableChatAutoScroll.INSTANCE.isEnabled()) return;
        original.call(instance, i);
    }

    @Inject(
        method = "addMessageToDisplayQueue",
        at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V", shift = At.Shift.AFTER)
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

    @Shadow
    private List<GuiMessage> allMessages = new LinkedList<>();

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public static int getHeight(double d) {
        return 0;
    }

    @Unique
    private GuiMessage lastHovered = null;

    @Override
    public GuiMessage devonian$getLastHoveredMessage() {
        return lastHovered;
    }

    @Override
    public void devonian$setLastHoveredMessage(GuiMessage msg) {
        lastHovered = msg;
    }

    @WrapOperation(method = "getHeight()I", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;getHeight(D)I"))
    private int devonian$onGetHeight(double d, Operation<Integer> original) {
        if (!PeekChatKeybind.INSTANCE.isEnabled()) return original.call(d);
        return getHeight(PeekChatKeybind.INSTANCE.getKeybind().isDown() ? minecraft.options.chatHeightFocused().get() : d);
    }
}
