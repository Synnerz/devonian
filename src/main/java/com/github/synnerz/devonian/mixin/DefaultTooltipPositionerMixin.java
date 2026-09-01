package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.inventory.ScrollableTooltip;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DefaultTooltipPositioner.class)
public class DefaultTooltipPositionerMixin {
    @Definition(id = "paddedHeight", local = @Local(type = int.class, name = "paddedHeight"))
    @Definition(id = "screenHeight", local = @Local(type = int.class, name = "screenHeight", argsOnly = true))
    @Definition(id = "result", local = @Local(type = Vector2i.class, name = "result", argsOnly = true))
    @Definition(id = "y", field = "Lorg/joml/Vector2i;y:I")
    @Expression("result.y + paddedHeight > screenHeight")
    @ModifyExpressionValue(
        method = "positionTooltip(IILorg/joml/Vector2i;II)V",
        at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private boolean devonian$positionTooltip(boolean original) {
        return original && !ScrollableTooltip.INSTANCE.getSETTING_START_TOP().get();
    }
}
