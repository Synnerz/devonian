#version 330

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    #moj_import <minecraft:fog.glsl>
#endif

#moj_import <minecraft:dynamictransforms.glsl>

// doesn't do anything right now (moj_import) is imported before defines but maybe it will in the future :)
#ifdef DEVONIAN_CHROMA_TEXT
    #moj_import <devonian:chroma.glsl>
#endif

uniform sampler2D Sampler0;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    in float sphericalVertexDistance;
    in float cylindricalVertexDistance;
#endif

in vec4 vertexColor;
in vec2 texCoord0;

#ifdef DEVONIAN_CHROMA_TEXT
    in vec4 origColor;
#endif

out vec4 fragColor;

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
    if (color.a < 0.1) discard;

    #ifdef IS_SEE_THROUGH
        fragColor = color * ColorModulator;
    #elif defined(IS_GUI)
        fragColor = color;
    #else
        fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
    #endif
}
