precision mediump float;
uniform float u_time;
uniform vec2 u_resolution;
uniform float u_emotions[6]; // 0=Peur, 1=Colere, 2=Tristesse, 3=Joie, 4=Calme, 5=Fatigue

// Pseudo-random noise helper
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// 2D Value Noise
float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);

    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(a, b, u.x) +
            (c - a) * u.y * (1.0 - u.x) +
            (d - b) * u.x * u.y;
}

// Fractal Brownian Motion (fBm) for organic details
float fbm(vec2 st) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 3; i++) {
        value += amplitude * noise(st);
        st *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 st = gl_FragCoord.xy / u_resolution.xy;
    st.x *= u_resolution.x / u_resolution.y;
    
    // axial symmetry in the screen center to mirror Rorschach test
    st.x = abs(st.x - 0.5);

    // animation speed modulated by emotions
    float baseSpeed = 0.35;
    float emotionSpeed = u_emotions[1] * 0.8 + u_emotions[0] * 0.4 - u_emotions[2] * 0.3 - u_emotions[5] * 0.2;
    float speed = u_time * (baseSpeed + emotionSpeed);

    // coordinate distortion representing mental tension/fear
    vec2 offset = vec2(
        fbm(st * 1.8 + vec2(speed * 0.4, speed * 0.2)),
        fbm(st * 1.8 - vec2(speed * 0.1, speed * 0.5))
    ) * (0.12 + u_emotions[0] * 0.15);

    vec2 p = st + offset;

    // Metaballs algorithm for fluid blots
    float dist = length(vec2(p.x, p.y - 0.5));
    float noiseVal = fbm(p * (4.0 - u_emotions[4] * 1.5) + vec2(speed * 0.8));
    
    // threshold definition (fear breaks blots, calmness stabilizes them)
    float threshold = 0.38 + u_emotions[0] * 0.12 - u_emotions[4] * 0.08;
    
    // blur representing drowsiness/dissociation
    float blur = 0.02 + u_emotions[5] * 0.08;
    float alpha = smoothstep(threshold + blur, threshold, noiseVal - dist * 0.75);

    // color blending
    vec3 baseColor = vec3(0.03, 0.03, 0.05); // neutral dark dark blue-purple
    
    vec3 blotchColor = vec3(
        u_emotions[1] * 0.75 + u_emotions[3] * 0.3,                    // R
        u_emotions[3] * 0.55 + u_emotions[4] * 0.45,                   // G
        u_emotions[2] * 0.65 + u_emotions[0] * 0.35 + u_emotions[5] * 0.25 // B
    );

    // Fallback if emotions are flat
    if (length(blotchColor) < 0.1) {
        blotchColor = vec3(0.15, 0.12, 0.18);
    }

    vec3 finalColor = mix(baseColor, blotchColor, alpha);

    gl_FragColor = vec4(finalColor, 1.0);
}
