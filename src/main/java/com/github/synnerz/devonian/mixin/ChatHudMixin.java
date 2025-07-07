package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveChatLimit;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Redirect(
            method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;size()I")
    )
    private int devonian$onMessageSize(List instance) {
        if (RemoveChatLimit.INSTANCE.isEnabled() && instance.size() > 100) return 100;
        return instance.size();
    }

    @Redirect(
            method = "addVisibleMessage",
            at = @At(value = "INVOKE", target = "Ljava/util/List;size()I")
    )
    private int devonian$onMessageVisibleSize(List instance) {
        if (RemoveChatLimit.INSTANCE.isEnabled() && instance.size() > 100) return 100;
        return instance.size();
    }
}
