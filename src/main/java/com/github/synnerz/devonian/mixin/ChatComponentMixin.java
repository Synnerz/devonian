package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.ChatComponentAccessor2;
import com.github.synnerz.devonian.api.ChatUtils;
import com.github.synnerz.devonian.features.misc.DisableChatAutoScroll;
import com.github.synnerz.devonian.features.misc.DisableChatReset;
import com.github.synnerz.devonian.features.misc.PeekChatKeybind;
import com.github.synnerz.devonian.features.misc.RemoveChatLimit;
import com.github.synnerz.devonian.features.misc.chat.CompactChat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin implements ChatComponentAccessor2 {
    @Shadow
    private List<GuiMessage.Line> trimmedMessages = new LinkedList<>();

    @ModifyExpressionValue(
        method = { "addMessageToQueue", "addMessageToDisplayQueue" },
        at = @At(value = "CONSTANT", args = "intValue=100")
    )
    private int devonian$removeChatLimit(int constant) {
        if (!RemoveChatLimit.INSTANCE.isEnabled()) return constant;
        return RemoveChatLimit.INSTANCE.getSETTING_MAX_MESSAGES().get().intValue();
    }

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true, name = "contents")
    private Component devonian$addMessage(Component contents) {
        if (contents == null) return contents;
        return CompactChat.INSTANCE.compactText(contents);
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void devonian$clearMessages(boolean bl, CallbackInfo ci) {
        CompactChat.INSTANCE.clearHistory();
    }

    @Inject(
            method = "clearMessages",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;flushQueue()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void devonian$clearMessagesPost(boolean history, CallbackInfo ci) {
        if (!DisableChatReset.INSTANCE.isEnabled()) return;

        ci.cancel();
    }

    @WrapOperation(
        method = "addMessageToDisplayQueue",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;scrollChat(I)V")
    )
    private void devonian$onChatScroll(ChatComponent instance, int i, Operation<Void> original) {
        if (DisableChatAutoScroll.INSTANCE.isEnabled()) return;
        original.call(instance, i);
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

    @Shadow
    private int chatScrollbarPos;

    @Shadow
    protected abstract void addMessageToDisplayQueue(GuiMessage message);

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

    @WrapOperation(
        method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;forEachLine(Lnet/minecraft/client/gui/components/ChatComponent$AlphaCalculator;Lnet/minecraft/client/gui/components/ChatComponent$LineConsumer;)I")
    )
    private int devonian$checkHoveredMessage(ChatComponent instance, ChatComponent.AlphaCalculator alphaCalculator, ChatComponent.LineConsumer lineConsumer, Operation<Integer> original) {
        lastHovered = null;
        return original.call(instance, alphaCalculator, lineConsumer);
    }

    @WrapOperation(method = "getHeight()I", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;getHeight(D)I"))
    private int devonian$onGetHeight(double d, Operation<Integer> original) {
        if (!PeekChatKeybind.INSTANCE.isEnabled()) return original.call(d);
        return getHeight(PeekChatKeybind.INSTANCE.getKeybind().isDown() ? minecraft.options.chatHeightFocused().get() : d);
    }

    @Unique
    private ListIterator<GuiMessage.Line> trimmedIter = null;

    @ModifyVariable(
        method = "forEachLine",
        at = @At(value = "STORE"),
        name = "i"
    )
    private int devonian$trimmedIteratorStart(int i) {
        trimmedIter = trimmedMessages.listIterator(i + chatScrollbarPos);
        return i;
    }

    @WrapOperation(
        method = "forEachLine",
        at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;")
    )
    private <E> E devonian$trimmedIteratorGet(List<GuiMessage.Line> instance, int i, Operation<GuiMessage.Line> original) {
        while (i > trimmedIter.previousIndex()) trimmedIter.next();
        while (i < trimmedIter.previousIndex()) trimmedIter.previous();
        @SuppressWarnings("unchecked")
        E obj = (E) trimmedIter.previous();
        return obj;
    }

    @Inject(
        method = "forEachLine",
        at = @At(value = "TAIL")
    )
    private void devonian$trimmedIterEnd(ChatComponent.AlphaCalculator alphaCalculator, ChatComponent.LineConsumer lineConsumer, CallbackInfoReturnable<Integer> cir) {
        trimmedIter = null;
    }

    @Unique
    private ListIterator<GuiMessage.Line> injectedIterator;

    @Override
    public void devonian$injectAddMessage(GuiMessage message, ListIterator<GuiMessage.Line> iterator) {
        injectedIterator = iterator;
        addMessageToDisplayQueue(message);
        injectedIterator = null;
    }

    @WrapOperation(
        method = "addMessageToDisplayQueue",
        at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V")
    )
    private <E> void devonian$injectAddMessageAdd(List<E> instance, E e, Operation<Void> original) {
        if (injectedIterator != null) injectedIterator.add((GuiMessage.Line) e);
        else original.call(instance, e);
    }
}
