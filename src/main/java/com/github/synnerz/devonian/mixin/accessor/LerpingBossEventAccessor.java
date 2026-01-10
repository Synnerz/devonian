package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LerpingBossEvent.class)
public interface LerpingBossEventAccessor {
    @Accessor("targetPercent")
    float getTargetPercent();
}
