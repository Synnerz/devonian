package com.github.synnerz.devonian.mixin;

import com.github.synnerz.devonian.api.ImageTransfer;
import com.github.synnerz.devonian.features.misc.AutoCopyScreenshot;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {
    @WrapMethod(method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V")
    private static void devonian$onScreenshot(RenderTarget renderTarget, int i, Consumer<NativeImage> consumer, Operation<Void> original) {
        Consumer<NativeImage> con = (nativeImage) -> {
            if (AutoCopyScreenshot.INSTANCE.isEnabled() && !System.getProperty("os.name").toLowerCase().contains("mac")) {
                try {
                    int[] pixels = nativeImage.getPixelsABGR();
                    int w = nativeImage.getWidth();
                    int h = nativeImage.getHeight();
                    new Thread(() -> {
                        var buf = new DataBufferInt(pixels, pixels.length);
                        int[] bands = new int[] {
                            0x000000FF,
                            0x0000FF00,
                            0x00FF0000,
                            0xFF000000,
                        };
                        var raster = Raster.createPackedRaster(
                            buf,
                            w, h,
                            w,
                            bands,
                            null
                        );
                        var bimg = new BufferedImage(
                            new DirectColorModel(32, bands[0], bands[1], bands[2], bands[3]),
                            raster,
                            false,
                            null
                        );

                        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(new ImageTransfer(bimg), null);
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
