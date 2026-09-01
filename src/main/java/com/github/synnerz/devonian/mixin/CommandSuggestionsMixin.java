package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.chat.CommandAliases;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CommandSuggestions.class, priority = 1001)
public class CommandSuggestionsMixin {
    @WrapOperation(
            method = "updateCommandInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/CommandDispatcher;parse(Lcom/mojang/brigadier/StringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/ParseResults;"
            )
    )
    private ParseResults<ClientSuggestionProvider> devonian$onUpdateCommandInfo(
            CommandDispatcher<ClientSuggestionProvider> instance, StringReader command, Object source,
            Operation<ParseResults<ClientSuggestionProvider>> original,
            @Local(name = "commands") CommandDispatcher<ClientSuggestionProvider> cmd
    ) {
        var aliases = CommandAliases.INSTANCE.getAliases();
        if (!aliases.isEmpty()) {
            aliases.forEach((alias) -> {
                cmd.register(LiteralArgumentBuilder.literal(alias));
            });
            return instance.parse(command, (ClientSuggestionProvider) source);
        }

        return original.call(instance, command, source);
    }
}
