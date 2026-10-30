#version 330

// Cyberspace screen grade: pulls the world toward a dark violet floor and a teal highlight, keeps a quarter of the
// original colour so the scene stays readable, then adds faint scanlines, edge colour fringing and a vignette.

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform CyberConfig {
    vec4 TintStrength;
    vec4 Shadow;
};

out vec4 fragColor;

void main() {
    vec2 centred = texCoord - 0.5;
    float edge = dot(centred, centred);

    vec2 shift = centred * edge * 0.012;
    vec3 colour = vec3(
        texture(InSampler, texCoord + shift).r,
        texture(InSampler, texCoord).g,
        texture(InSampler, texCoord - shift).b);

    float luma = dot(colour, vec3(0.299, 0.587, 0.114));
    vec3 graded = mix(Shadow.rgb, TintStrength.rgb, smoothstep(0.02, 0.95, luma)) + colour * 0.25;
    vec3 outColour = mix(colour, graded, TintStrength.a);

    float scan = 0.93 + 0.07 * step(1.5, mod(texCoord.y * OutSize.y, 3.0));
    float vignette = mix(0.62, 1.0, smoothstep(0.55, 0.05, edge * 1.6));

    fragColor = vec4(outColour * scan * vignette, 1.0);
}
