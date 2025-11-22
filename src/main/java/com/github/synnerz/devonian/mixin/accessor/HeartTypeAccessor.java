package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.HeartType.class)
public interface HeartTypeAccessor {
    @Invoker("forPlayer")
    static Gui.HeartType invokeForPlayer(Player player) {
        throw new AssertionError();
    }
}
