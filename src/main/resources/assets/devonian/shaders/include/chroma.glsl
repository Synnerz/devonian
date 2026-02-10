#version 330

#moj_import <minecraft:globals.glsl>

layout(std140) uniform DevonianChromaInfo {
    float timeOffset;
    float chromaSize;
    float chromaSpeed;
    float chromaLightness;
    float chromaChroma;
};

#define ONE_OVER_256 0.00390625
const mat3 M2_1 = mat3(
    1.0, 1.0, 1.0,
    0.3963377774, -0.1055613458, -0.0894841775,
    0.2158037573, -0.0638541728, -1.2914855480
);
const mat3 lmsToRgb = mat3(
    4.0767416621, -1.2684380046, -0.0041960863,
    -3.3077115913, 2.6097574011, -0.7034186147,
    0.2309699292, -0.3413193965, 1.7076147010
);

const vec3 CHROMA_COLOR = vec3(171.0, 205.0, 239.0) / 255.0;
const vec3 CHROMA_SHADOW_COLOR = vec3(254.0, 220.0, 186.0) / 255.0;

bool isSameColor(vec3 c1, vec3 c2) {
    return all(lessThanEqual(abs(c1 - c2), vec3(0.00001)));
}

vec4 doTransformChroma(vec4 orig, float lightness) {
    float hue = ((gl_FragCoord.x - gl_FragCoord.y) / min(ScreenSize.x, ScreenSize.y) * chromaSize) - timeOffset * chromaSpeed;

    vec3 Lab = vec3(
        lightness,
        chromaChroma * cos(hue),
        chromaChroma * sin(hue)
    );
    vec3 lms = M2_1 * Lab;
    lms = lms * lms * lms;
    vec4 color = vec4(lmsToRgb * lms, orig.a);
    
    return color;
}

vec4 dv_transformChroma(vec4 orig) {
    if (isSameColor(orig.rgb, CHROMA_COLOR)) return doTransformChroma(orig, chromaLightness);
    if (isSameColor(orig.rgb, CHROMA_SHADOW_COLOR)) return doTransformChroma(orig, chromaLightness * 0.25);
    return orig;
}