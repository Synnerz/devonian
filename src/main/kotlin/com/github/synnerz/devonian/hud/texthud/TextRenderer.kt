package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.utils.TextRendererImpl
import com.github.synnerz.devonian.utils.TextRendererImpl.TextRenderParams
import java.awt.image.BufferedImage

class TextRenderer(name: String) : BufferedImageRenderer<TextRenderParams>(name) {
    override fun drawImage(img: BufferedImage, param: TextRenderParams): BufferedImage = TextRendererImpl.drawImage(img, param)
}