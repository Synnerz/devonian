package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.ChangeCrouchHeight;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.entity.Avatar.DEFAULT_VEHICLE_ATTACHMENT;

@Mixin(Avatar.class)
public class AvatarMixin {
    @Unique
    private final EntityDimensions OLD_CROUCH = EntityDimensions
        .scalable(0.6f, 1.8f)
        .withEyeHeight(1.54f)
        .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, DEFAULT_VEHICLE_ATTACHMENT));

    @Inject(
        method = "getDefaultDimensions",
        at = @At("HEAD"),
        cancellable = true
    )
    private void devonian$sneakHeight(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!ChangeCrouchHeight.INSTANCE.isEnabled()) return;
        if (!ChangeCrouchHeight.INSTANCE.changeNonVisual()) return;
        Entity that = (Entity) (Object) this;
        if (!(that instanceof LocalPlayer)) return;
        if (pose == Pose.CROUCHING) cir.setReturnValue(OLD_CROUCH);
    }
}
