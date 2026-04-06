package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.PreExtractRenderEntityEvent;
import com.github.synnerz.devonian.api.events.PreRenderEntityEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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
            at = @At("HEAD")
    )
    private <S extends EntityRenderState> void devonian$preRenderEntity(S renderState, CameraRenderState camera, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        new PreRenderEntityEvent(renderState, camera, poseStack, submitNodeCollector).post();
    }

    @Inject(
        method = "extractEntity",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$preExtractRenderEntity(Entity entity, float f, CallbackInfoReturnable<EntityRenderState> cir) {
        PreExtractRenderEntityEvent event = new PreExtractRenderEntityEvent(entity, f);
        if (event.post()) {
            EntityRenderState noop = new EntityRenderState();
            noop.entityType = EntityType.AREA_EFFECT_CLOUD;
            cir.setReturnValue(noop);
        }
    }
}
