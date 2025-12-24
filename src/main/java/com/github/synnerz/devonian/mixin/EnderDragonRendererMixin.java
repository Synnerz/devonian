package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.dungeons.m7.RecolorDragons;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonRenderer.class)
public class EnderDragonRendererMixin {
    @Unique
    private final RenderStateDataKey<Integer> idKey = RenderStateDataKey.create(() -> "devonian$recolorDragonsId");

    @WrapOperation(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;pack(FZ)I")
    )
    private int devonian$recolorDragons(float f, boolean bl, Operation<Integer> original, @Local(argsOnly = true) EnderDragonRenderState state) {
        if (RecolorDragons.INSTANCE.isEnabled()) {
            Integer id = RecolorDragons.INSTANCE.getColorId(state.getData(idKey));
            if (id != null) return OverlayTexture.pack(15 - RecolorDragons.INSTANCE.getCOLORS().length + id, 12);
        }
        return original.call(f, bl);
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;F)V",
        at = @At("TAIL")
    )
    private void devonian$getEntityId(EnderDragon enderDragon, EnderDragonRenderState enderDragonRenderState, float f, CallbackInfo ci) {
        enderDragonRenderState.setData(idKey, enderDragon.getId());
    }
}
