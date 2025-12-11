package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.utils.TextRendererImpl
import java.awt.image.BufferedImage

class TextRenderer(name: String) : BufferedImageRenderer<TextRenderer.RenderParams>(name) {
    override fun drawImage(img: BufferedImage, param: RenderParams): BufferedImage = TextRendererImpl.drawImage(img, param)

    class RenderParams(
        val renderParams: StylizedTextHud.TextRenderParams,
        val lines: List<StringParser.LayoutLineData>,
        width: Float,
        visualWidth: Float,
        ascent: Float,
        descent: Float,
    ) : StylizedTextHud.FontMetrics(
        width,
        visualWidth,
        ascent,
        descent,
    )
}