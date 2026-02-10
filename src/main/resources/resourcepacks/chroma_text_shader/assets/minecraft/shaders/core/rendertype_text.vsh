#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

    #ifdef DEVONIAN_CHROMA_TEXT
out vec4 origColor;
    #endif

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
        #ifdef DEVONIAN_CHROMA_TEXT
    vertexColor = texelFetch(Sampler2, UV2 / 16, 0);
        #else
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
        #endif
    texCoord0 = UV0;

        #ifdef DEVONIAN_CHROMA_TEXT
    origColor = Color;
        #endif
}
