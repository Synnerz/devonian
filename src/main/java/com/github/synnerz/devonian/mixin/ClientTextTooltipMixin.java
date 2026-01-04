package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.ClientTextTooltipStringAccessor;
import com.github.synnerz.devonian.utils.StringUtils;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientTextTooltip.class)
public abstract class ClientTextTooltipMixin implements ClientTextTooltipStringAccessor {
    @Unique
    private String strCache = null;

    @Unique
    private boolean hasCache = false;

    @Override
    public @Nullable String devonian$asString() {
        if (!hasCache) {
            strCache = StringUtils.INSTANCE.tooltipAsString((ClientTextTooltip) (Object) this);
            hasCache = true;
        }
        return strCache;
    }
}
