#version 330
#extension GL_ARB_separate_shader_objects : require

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    #include <minecraft:fog.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>

#ifdef DEVONIAN_CHROMA_TEXT
    #include <devonian:chroma.glsl>
#endif

uniform sampler2D Sampler0;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    layout(location = 0) in float sphericalVertexDistance;
    layout(location = 1) in float cylindricalVertexDistance;
#endif

layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;

#ifdef DEVONIAN_CHROMA_TEXT
    layout(location = 4) in vec4 origColor;
#endif

layout(location = 0) out vec4 fragColor;

void main() {
    #ifdef IS_GRAYSCALE
        vec4 texColor = texture(Sampler0, texCoord0).rrrr;
    #else
        vec4 texColor = texture(Sampler0, texCoord0);

        #ifdef DEVONIAN_CHROMA_TEXT
            vec4 trf = dv_transformChroma(origColor);

            if (trf != origColor) {
                texColor = texColor * trf;
            }
        #endif

    #endif

    #ifdef IS_SEE_THROUGH
        vec4 color = texColor * vertexColor;
    #else
        vec4 color = texColor * vertexColor * ColorModulator;
    #endif
    if (color.a < 0.1) {
        discard;
    }

    #ifdef IS_SEE_THROUGH
        fragColor = color * ColorModulator;
    #elif defined(IS_GUI)
        fragColor = color;
    #else
        fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
    #endif
}