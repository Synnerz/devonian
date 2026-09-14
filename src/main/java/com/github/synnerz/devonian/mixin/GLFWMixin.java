package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.features.misc.inventory.NoCursorReset;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDL_Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SDLEvents.class)
public class GLFWMixin {
    // TODO: double check that this actually works
    @Inject(
        method = "SDL_PollEvent",
        at = @At("TAIL"),
        remap = false
    )
    private static void devonian$noCursorReset(SDL_Event event, CallbackInfoReturnable<Boolean> cir) {
        NoCursorReset.ignoreFirstBatch--;
    }
}
