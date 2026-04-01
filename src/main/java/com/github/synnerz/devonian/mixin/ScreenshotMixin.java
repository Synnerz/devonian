package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.ImageTransfer;
import com.github.synnerz.devonian.features.misc.AutoCopyScreenshot;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {
    @Unique
    private static BufferedImage toBufr(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage bfr = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getPixel(x, y);
                bfr.setRGB(x, y, argb);
            }
        }

        return bfr;
    }

    @WrapMethod(method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V")
    private static void devonian$onScreenshot(RenderTarget renderTarget, int i, Consumer<NativeImage> consumer, Operation<Void> original) {
        Consumer<NativeImage> con = (nativeImage) -> {
            if (AutoCopyScreenshot.INSTANCE.isEnabled() && !System.getProperty("os.name").toLowerCase().contains("mac")) {
                try {
                    new Thread(() -> {
                        var itr = new ImageTransfer(toBufr(nativeImage));
                        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(itr, null);
                    }).start();
                } catch (Exception e) {
                    System.out.println("Devonian$AutoCopyScreenshot");
                    e.printStackTrace();
                }
            }

            consumer.accept(nativeImage);
        };

        original.call(renderTarget, i, con);
    }
}
