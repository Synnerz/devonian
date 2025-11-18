package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.RemoveChatLimit;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
}
