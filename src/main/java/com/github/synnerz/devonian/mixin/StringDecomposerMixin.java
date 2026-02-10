package com.github.synnerz.devonian.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StringDecomposer.class)
public class StringDecomposerMixin {
    @Unique
    private static final TextColor CHROMA_COLOR = TextColor.fromRgb(0xABCDEF);
    @Unique
    private static final int CHROMA_SHADOW_COLOR = 0xFFFEDCBA;

    @WrapOperation(
        method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/ChatFormatting;getByCode(C)Lnet/minecraft/ChatFormatting;")
    )
    private static ChatFormatting devonian$chromaText(char c, Operation<ChatFormatting> original, @Local(ordinal = 2) LocalRef<Style> style3) {
        ChatFormatting o = original.call(c);

        if (Character.toLowerCase(c) == 'z') {
            Style old = style3.get();
            style3.set(
                new Style(
                    CHROMA_COLOR,
                    old.getShadowColor() != null ? old.getShadowColor() : CHROMA_SHADOW_COLOR,
                    false, false, false, false, false,
                    old.getClickEvent(),
                    old.getHoverEvent(),
                    old.getInsertion(),
                    old.getFont()
                )
            );
        }

        return o;
    }
}
