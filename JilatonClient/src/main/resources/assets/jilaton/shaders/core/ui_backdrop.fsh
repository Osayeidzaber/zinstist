#version 330

// Jilaton animated UI backdrop. Native GLSL takes on React Bits backgrounds (Dark Veil, Aurora, Threads, Beams,
// Galaxy, Liquid Chrome), written from scratch for Minecraft's GUI pipeline. Per-frame inputs arrive through a
// 4x1 data texture (Sampler0) because GUI blits carry no custom uniforms:
//   texel 0: time in ms, 32-bit big-endian across RGBA
//   texel 1: mouse x (RG, 16-bit), mouse y (BA, 16-bit), both 0..1 of the quad
//   texel 2: accent RGB, A = intensity
//   texel 3: R = mode, G = quality (0 low, 1 high), BA = aspect (w/h * 4096, 16-bit)
// texCoord0 runs 0..1 across the quad.

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

ivec4 bytesAt(int i) {
    return ivec4(round(texelFetch(Sampler0, ivec2(i, 0), 0) * 255.0));
}

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), u.x), mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), u.x), u.y);
}

float fbm(vec2 p, int octaves) {
    float v = 0.0, a = 0.5;
    mat2 r = mat2(0.8, -0.6, 0.6, 0.8);
    for (int i = 0; i < 6; i++) {
        if (i >= octaves) break;
        v += a * noise(p);
        p = r * p * 2.02 + 3.1;
        a *= 0.5;
    }
    return v;
}

// Dark Veil: slow domain-warped fog, lit from the cursor.
vec3 darkVeil(vec2 p, vec2 m, float t, vec3 accent, int oct) {
    vec2 q = vec2(fbm(p * 1.4 + t * 0.03, oct), fbm(p * 1.4 - t * 0.025 + 5.2, oct));
    vec2 r = vec2(fbm(p * 1.8 + 3.0 * q + t * 0.05, oct), fbm(p * 1.8 + 3.0 * q - t * 0.04 + 8.3, oct));
    float f = fbm(p * 1.2 + 2.6 * r, oct);
    float light = exp(-3.2 * length(p - m));
    vec3 col = mix(vec3(0.015), vec3(0.16), smoothstep(0.35, 0.95, f));
    col += accent * 0.22 * smoothstep(0.5, 1.0, f) * (0.35 + light);
    col += vec3(0.06) * light;
    return col;
}

// Aurora: soft vertical curtains drifting sideways, brighter near the cursor column.
vec3 aurora(vec2 p, vec2 m, float t, vec3 accent, int oct) {
    vec3 col = vec3(0.01);
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float x = p.x * (1.2 + fi * 0.35) + t * (0.04 + fi * 0.015) + fi * 1.7;
        float wave = fbm(vec2(x, t * 0.05 + fi), oct);
        float center = 0.15 + 0.35 * wave + fi * 0.08;
        float band = exp(-pow((p.y - center) * (5.0 - fi), 2.0));
        float streak = 0.6 + 0.4 * noise(vec2(p.x * 40.0 + t * 0.2, fi));
        vec3 tint = mix(vec3(0.85), accent, 0.35 + 0.2 * fi);
        col += tint * band * streak * (0.10 + 0.05 * fi);
    }
    col *= 0.8 + 0.6 * exp(-6.0 * abs(p.x - m.x));
    return col;
}

// Threads: many thin flowing lines that bend away from the cursor.
vec3 threads(vec2 p, vec2 m, float t, vec3 accent) {
    vec3 col = vec3(0.008);
    float push = exp(-8.0 * length(p - m));
    for (int i = 0; i < 18; i++) {
        float fi = float(i);
        float y = 0.18 + fi * 0.035;
        y += 0.06 * sin(p.x * (2.0 + fi * 0.13) + t * (0.35 + fi * 0.01) + fi);
        y += 0.03 * sin(p.x * 7.0 - t * 0.6 + fi * 2.1);
        y += 0.08 * push * sign(p.y - m.y + 1e-4);
        float d = abs(p.y - y);
        float line = smoothstep(0.0025, 0.0, d) + 0.25 * smoothstep(0.02, 0.0, d);
        float fade = smoothstep(0.0, 0.3, p.x) * smoothstep(1.6, 0.9, p.x);
        col += mix(vec3(0.55), accent, fi / 18.0 * 0.6) * line * fade * 0.22;
    }
    return col;
}

// Beams: tall light shafts sweeping slowly, like stage light through haze.
vec3 beams(vec2 p, vec2 m, float t, vec3 accent, int oct) {
    vec3 col = vec3(0.01);
    float haze = fbm(p * 2.0 + vec2(0.0, -t * 0.08), oct);
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        float x0 = fi * 0.32 + 0.1 * sin(t * 0.12 + fi * 1.3) + (m.x - 0.5) * 0.08 * (fi - 2.5);
        float skew = (p.x - x0) + (p.y - 1.0) * 0.35;
        float beam = exp(-pow(skew * 18.0, 2.0));
        col += mix(vec3(0.7), accent, 0.4) * beam * (0.08 + 0.12 * haze) * (1.0 - p.y * 0.6);
    }
    col += vec3(0.04) * haze * haze;
    return col;
}

// Galaxy: three parallax star layers with twinkle, shifting against the cursor.
vec3 galaxy(vec2 p, vec2 m, float t, vec3 accent) {
    vec3 col = vec3(0.004);
    for (int layer = 0; layer < 3; layer++) {
        float fl = float(layer);
        float scale = 28.0 + fl * 22.0;
        vec2 q = (p + (m - 0.5) * (0.015 + fl * 0.02) + vec2(t * 0.004 * (fl + 1.0), 0.0)) * scale;
        vec2 cell = floor(q), f = fract(q) - 0.5;
        float h = hash(cell + fl * 17.0);
        if (h > 0.88) {
            vec2 off = vec2(hash(cell + 3.1), hash(cell + 7.7)) - 0.5;
            float d = length(f - off * 0.6);
            float tw = 0.6 + 0.4 * sin(t * (1.0 + h * 3.0) + h * 40.0);
            float star = smoothstep(0.12 - fl * 0.02, 0.0, d) * tw;
            col += mix(vec3(1.0), accent, h > 0.97 ? 0.6 : 0.0) * star * (0.5 + 0.25 * fl);
        }
    }
    col += accent * 0.05 * exp(-4.0 * length(p - vec2(0.9, 0.35)));
    return col;
}

// Liquid Chrome: a slow metallic marble in greyscale with bright specular ridges.
vec3 chrome(vec2 p, vec2 m, float t, vec3 accent, int oct) {
    vec2 q = p * 2.2 + (m - 0.5) * 0.15;
    float a = fbm(q + vec2(t * 0.05, -t * 0.04), oct);
    float b = fbm(q * 1.7 + a * 2.4 - t * 0.03, oct);
    float ridge = 1.0 - abs(sin(b * 9.0 + a * 4.0));
    float spec = pow(ridge, 6.0);
    vec3 col = vec3(0.02 + 0.10 * b) + vec3(0.55) * spec * 0.55;
    col += accent * spec * 0.12;
    return col;
}

void main() {
    ivec4 tb = bytesAt(0);
    float t = float((tb.r << 24) | (tb.g << 16) | (tb.b << 8) | tb.a) / 1000.0;
    ivec4 mb = bytesAt(1);
    vec2 m = vec2(float(mb.r * 256 + mb.g), float(mb.b * 256 + mb.a)) / 65535.0;
    vec4 ac = texelFetch(Sampler0, ivec2(2, 0), 0);
    vec3 accent = ac.rgb;
    float intensity = ac.a;
    ivec4 cfg = bytesAt(3);
    int mode = cfg.r;
    int oct = cfg.g > 0 ? 5 : 3;
    float aspect = float(cfg.b * 256 + cfg.a) / 4096.0;

    vec2 uv = texCoord0;
    vec2 p = vec2(uv.x * aspect, 1.0 - uv.y);
    vec2 mp = vec2(m.x * aspect, 1.0 - m.y);

    vec3 col;
    if (mode == 0) col = darkVeil(p, mp, t, accent, oct);
    else if (mode == 1) col = aurora(p, mp, t, accent, oct);
    else if (mode == 2) col = threads(p, mp, t, accent);
    else if (mode == 3) col = beams(vec2(uv.x * aspect, uv.y), vec2(mp.x, m.y), t, accent, oct);
    else if (mode == 4) col = galaxy(p, mp, t, accent);
    else col = chrome(p, mp, t, accent, oct);

    col *= intensity;
    // Vignette and film grain keep every mode deep at the edges and free of banding.
    vec2 c = uv - 0.5;
    col *= 1.0 - 0.75 * dot(c, c) * 1.6;
    col += (hash(uv * 1000.0 + fract(t)) - 0.5) / 255.0 * 2.0;
    fragColor = vec4(max(col, 0.0), 1.0) * vertexColor * ColorModulator;
}
