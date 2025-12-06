package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.BossBarHealth;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {
    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LerpingBossEvent;getName()Lnet/minecraft/network/chat/Component;")
    )
    private Component devonian$bossBarHealth(LerpingBossEvent instance, Operation<Component> original) {
        Component old = original.call(instance);
        if (old == null) return null;
        Component replacement = BossBarHealth.INSTANCE.changeBarName(old, instance);
        if (replacement == null) return old;
        return replacement;
    }
}
