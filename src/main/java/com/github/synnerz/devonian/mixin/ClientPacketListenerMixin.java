package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.Devonian;
import com.github.synnerz.devonian.api.events.ItemPickupEvent;
import com.github.synnerz.devonian.features.misc.chat.ChatEmotes;
import com.github.synnerz.devonian.features.misc.hiders.DisableWorldLoadingScreen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(
        method = "notifyPlayerLoaded",
        at = @At("HEAD")
    )
    private void devonian$disableWorldLoadingScreen(CallbackInfo ci) {
        if (!DisableWorldLoadingScreen.INSTANCE.isEnabled()) return;
        DisableWorldLoadingScreen.INSTANCE.onPlayerLoaded();
    }

    @WrapOperation(
        method = "handleTakeItemEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;getItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack devonian$itemPickupEventNormal(ItemEntity instance, Operation<ItemStack> original) {
        ItemStack stack = original.call(instance);
        new ItemPickupEvent(instance, instance.getId()).post();
        return stack;
    }

    // for deletors
    @WrapOperation(
        method = "method_64896",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getEntity(I)Lnet/minecraft/world/entity/Entity;")
    )
    private Entity devonian$itemPickupEventBackup(ClientLevel instance, int i, Operation<Entity> original) {
        Entity e = original.call(instance, i);
        if (e instanceof ItemEntity) {
            LocalPlayer p = Devonian.INSTANCE.getMinecraft().player;
            if (p != null && p.distanceToSqr(e) < 25.0) new ItemPickupEvent((ItemEntity) e, i).post();
        }
        return e;
    }

    @ModifyVariable(
        method = "sendChat",
        at = @At("HEAD"),
        argsOnly = true
    )
    private String devonian$chatEmotesChat(String string) {
        if (!ChatEmotes.INSTANCE.isEnabled()) return string;
        return ChatEmotes.INSTANCE.modifyMessage(string);
    }

    @ModifyVariable(
        method = "sendCommand",
        at = @At("HEAD"),
        argsOnly = true
    )
    private String devonian$chatEmotesCommand(String string) {
        if (!ChatEmotes.INSTANCE.isEnabled()) return string;
        return ChatEmotes.INSTANCE.modifyMessage(string);
    }
}
