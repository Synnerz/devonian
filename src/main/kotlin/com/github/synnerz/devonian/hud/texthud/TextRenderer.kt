package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.utils.render.impl.TextRendererImpl
import java.awt.image.BufferedImage

class TextRenderer(name: String) : BufferedImageRenderer<TextRenderer.RenderParams>(name) {
    override fun drawImage(img: BufferedImage, param: RenderParams): BufferedImage = TextRendererImpl.drawImage(img, param)

    class RenderParams(
        val renderParams: StylizedTextHud.TextRenderParams,
        val lines: List<StringParser.LayoutLineData>,
        width: Float,
        ascent: Float,
        descent: Float,
        val noAAHint: Boolean = false,
    ) : StylizedTextHud.FontMetrics(
        width,
        ascent,
        descent,
    )
}