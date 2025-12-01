package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.PreRenderEntityEvent;
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
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$preRenderEntity(Entity entity, float f, CallbackInfoReturnable<EntityRenderState> cir) {
        PreRenderEntityEvent event = new PreRenderEntityEvent(entity);
        if (event.post()) cir.setReturnValue(new EntityRenderState());
    }
}
