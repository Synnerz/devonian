package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.EventBus;
import com.github.synnerz.devonian.api.events.PreRenderEntityEvent;
import com.github.synnerz.devonian.api.events.RenderEntityEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(
            method = "submit",
            at = @At("HEAD"),
            cancellable = true
    )
    private void devonian$entityRender(EntityRenderState entityRenderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        EventBus.INSTANCE.post(new RenderEntityEvent(entityRenderState, cameraRenderState, poseStack, submitNodeCollector));
    }

    @Inject(
        method = "extractEntity",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$preRenderEntity(Entity entity, float f, CallbackInfoReturnable<EntityRenderState> cir) {
        PreRenderEntityEvent event = new PreRenderEntityEvent(entity);
        if (event.post()) {
            EntityRenderState noop = new EntityRenderState();
            noop.entityType = EntityType.AREA_EFFECT_CLOUD;
            cir.setReturnValue(noop);
        }
    }
}
