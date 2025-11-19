package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.inventory.NoCursorReset;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MouseHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @WrapWithCondition(
            method = {"releaseMouse", "grabMouse"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/MouseHandler;xpos:D",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private boolean devonian$setXCursor(MouseHandler instance, double value) {
        return NoCursorReset.INSTANCE.shouldReset();
    }

    @WrapWithCondition(
            method = {"releaseMouse", "grabMouse"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/MouseHandler;ypos:D",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private boolean devonian$setYCursor(MouseHandler instance, double value) {
        return NoCursorReset.INSTANCE.shouldReset();
    }
}
