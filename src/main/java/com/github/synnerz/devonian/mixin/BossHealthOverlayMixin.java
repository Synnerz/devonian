package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.bossbar.BossBar;
import com.github.synnerz.devonian.utils.BossEventWrapper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {
    @WrapOperation(
        method = "extractRenderState",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;")
    )
    private Collection<LerpingBossEvent> devonian$bossBarHealth(Map<UUID, LerpingBossEvent> instance, Operation<Collection<LerpingBossEvent>> original) {
        Collection<LerpingBossEvent> out = new ArrayList<>();
        original.call(instance).forEach(v -> {
            Component old = v.getName();
            Component replacement = BossBar.INSTANCE.changeBarName(old, v);
            if (replacement != null) {
                if (replacement == old) out.add(v);
                else out.add(new BossEventWrapper(v, replacement));
            }
        });
        return out;
    }
}
