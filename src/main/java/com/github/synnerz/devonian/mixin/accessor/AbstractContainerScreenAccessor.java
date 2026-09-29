package com.github.synnerz.devonian.mixin.accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Invoker("getHoveredSlot")
    Slot dv_getSlotAtPos(double mouseX, double mouseY);

    @Accessor("hoveredSlot")
    Slot dv_getHoveredSlot();

    @Accessor("leftPos")
    int dv_getLeftPos();

    @Accessor("topPos")
    int dv_getTopPos();

    @Accessor("leftPos")
    void dv_setLeftPos(int pos);

    @Accessor("topPos")
    void dv_setTopPos(int pos);
}
