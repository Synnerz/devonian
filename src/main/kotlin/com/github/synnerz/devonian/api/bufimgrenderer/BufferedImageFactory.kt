package com.github.synnerz.devonian.api.bufimgrenderer

import java.awt.image.BufferedImage

interface BufferedImageFactory {
    fun create(w: Int, h: Int): BufferedImage
}