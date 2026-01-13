package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.PostExtractRenderEntityEvent;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(
        method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
        at = @At("TAIL")
    )
    private void devonian$postExtractRenderEntity(Entity entity, float f, CallbackInfoReturnable<EntityRenderState> cir, @Local EntityRenderState state) {
        new PostExtractRenderEntityEvent(entity, state, f).post();
    }
}
