package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.DisableAttachedArrows;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class PlayerRendererMixin {
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void devonian$disableAttachedArrows(Avatar avatar, AvatarRenderState avatarRenderState, float f, CallbackInfo ci) {
        if (!DisableAttachedArrows.INSTANCE.isEnabled()) return;
        avatarRenderState.arrowCount = 0;
    }
}