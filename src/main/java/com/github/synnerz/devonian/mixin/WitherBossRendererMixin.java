package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.dungeons.WitherHighlight;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherBossRenderer.class)
public class WitherBossRendererMixin {
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/boss/wither/WitherBoss;Lnet/minecraft/client/renderer/entity/state/WitherRenderState;F)V",
        at = @At("TAIL")
    )
    private void devonian$witherHighlight(WitherBoss witherBoss, WitherRenderState witherRenderState, float f, CallbackInfo ci) {
        if (!WitherHighlight.INSTANCE.isEnabled()) return;
        WitherHighlight.INSTANCE.extractWither(witherBoss, witherRenderState);
    }
}
