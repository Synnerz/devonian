package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.events.GuiCharTypeEvent;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void devonian$onCharType(CharacterEvent characterEvent, CallbackInfoReturnable<Boolean> cir) {
        String str = characterEvent.codepointAsString();
        if (new GuiCharTypeEvent(characterEvent.codepoint(), str, characterEvent).post()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
