#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform vec4 ColorModulator;

out vec4 fragColor;

void main() {
    float dist = length(texCoord0);
    
    float outer = 1.0;
    float inner = 0.8;
    float thickness = 0.05;
    
    float circle = smoothstep(outer, outer - thickness, dist) * smoothstep(inner, inner + thickness, dist);
    
    if (circle <= 0.0) {
        discard;
    }
    
    fragColor = vertexColor * ColorModulator;
    fragColor.a *= circle;
}
