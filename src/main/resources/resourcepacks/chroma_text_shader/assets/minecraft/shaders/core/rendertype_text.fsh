#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
// doesn't do anything right now (moj_import) is imported before defines but maybe it will in the future :)
    #ifdef DEVONIAN_CHROMA_TEXT
#moj_import <devonian:chroma.glsl>
    #endif

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

    #ifdef DEVONIAN_CHROMA_TEXT
in vec4 origColor;
    #endif

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
        #ifdef DEVONIAN_CHROMA_TEXT
    texColor = texColor * dv_transformChroma(origColor);
        #endif
    vec4 color = texColor * vertexColor * ColorModulator;
    if (color.a < 0.1) discard;
    
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
