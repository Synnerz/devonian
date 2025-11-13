package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.inventory.NoCursorReset;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Mouse;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mouse.class)
public class MouseMixin {
    @WrapWithCondition(
            method = {"unlockCursor", "lockCursor"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Mouse;x:D",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private boolean devonian$setXCursor(Mouse instance, double value) {
        return NoCursorReset.INSTANCE.shouldReset();
    }

    @WrapWithCondition(
            method = {"unlockCursor", "lockCursor"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Mouse;y:D",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private boolean devonian$setYCursor(Mouse instance, double value) {
        return NoCursorReset.INSTANCE.shouldReset(true);
    }
}
