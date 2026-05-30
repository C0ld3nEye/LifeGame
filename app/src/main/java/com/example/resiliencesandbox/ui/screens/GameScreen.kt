package com.example.resiliencesandbox.ui.screens

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.resiliencesandbox.ui.GameViewModel

private const val SHADER_CODE = """
uniform float2 resolution;
uniform float time;
uniform float peur;
uniform float colere;
uniform float tristesse;
uniform float joie;
uniform float calme;
uniform float fatigue;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
float noise(float2 p) {
    float2 i = floor(p); float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + float2(0.0,0.0)), hash(i + float2(1.0,0.0)), u.x),
               mix(hash(i + float2(0.0,1.0)), hash(i + float2(1.0,1.0)), u.x), u.y);
}
float fbm(float2 p) {
    float f = 0.0;
    f += 0.5000 * noise(p); p *= 2.01;
    f += 0.2500 * noise(p); p *= 2.02;
    f += 0.1250 * noise(p); p *= 2.03;
    return f;
}
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution.xy;
    uv.x = abs(uv.x - 0.5); // Symétrie miroir parfaite
    
    float2 p = uv * 4.0;
    float t = time * 0.4;
    
    // Bruit fractal complexe
    float q = fbm(p - t * 0.3);
    float r = fbm(p + q + t * 0.2);
    
    // Distance depuis le centre de la symétrie
    float dist = length(uv - float2(0.0, 0.5));
    
    // Découpe franche pour un vrai effet d'encre (finis les halos flous)
    float ink = smoothstep(0.45, 0.43, dist + r * 0.4);
    
    // Gris argent de base
    float3 color = float3(0.5, 0.5, 0.55);
    color = mix(color, float3(0.8, 0.1, 0.1), colere * ink);
    color = mix(color, float3(0.1, 0.2, 0.5), tristesse * ink);
    color = mix(color, float3(0.9, 0.8, 0.2), joie * ink);
    
    return half4(color * ink, ink);
}
"""

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val characterState by viewModel.characterState.collectAsState()
    val narrativeText by viewModel.narrativeText.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()

    var inputText by remember { mutableStateOf("") }

    // Rorschach values mapping: (val / 100f) pour obtenir des floats entre 0.0 et 1.0
    val peur = (characterState?.peur ?: 0) / 100f
    val colere = (characterState?.colere ?: 0) / 100f
    val tristesse = (characterState?.tristesse ?: 0) / 100f
    val joie = (characterState?.joie ?: 0) / 100f
    val calme = (characterState?.calme ?: 0) / 100f
    val fatigue = (characterState?.fatigue ?: 0) / 100f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Le fond de l'application est noir profond
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. RorschachCanvas (~60% du haut de l'écran)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RorschachCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f),
                peur = peur,
                colere = colere,
                tristesse = tristesse,
                joie = joie,
                calme = calme,
                fatigue = fatigue
            )
        } else {
            // Fallback pour assurer la compilation sur SDK < 33 si existant
            Box(modifier = Modifier.weight(0.6f).fillMaxWidth().background(Color.DarkGray))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Narration textuelle (avec flou si isThinking)
        Text(
            text = narrativeText.ifEmpty { "La noirceur t'enveloppe. Ton esprit dérive..." },
            color = Color.LightGray,
            textAlign = TextAlign.Justify,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f)
                .verticalScroll(rememberScrollState())
                .then(if (isThinking) Modifier.blur(4.dp) else Modifier) // PAS de roue de chargement
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Input Utilisateur Minimaliste (Zéro chiffre)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E), // Fond anthracite
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent, // Sans bordures criardes
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                placeholder = { Text("Que fais-tu ?", color = Color.Gray) }
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isThinking) {
                        viewModel.submitPlayerAction(inputText)
                        inputText = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Envoyer",
                    tint = Color.White
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun RorschachCanvas(
    modifier: Modifier = Modifier,
    peur: Float,
    colere: Float,
    tristesse: Float,
    joie: Float,
    calme: Float,
    fatigue: Float
) {
    val shader = remember { RuntimeShader(SHADER_CODE) }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (true) {
            withFrameMillis { frameTime ->
                time = (frameTime - startTime) / 1000f
            }
        }
    }

    Box(
        modifier = modifier.drawWithCache {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("time", time)
            shader.setFloatUniform("peur", peur)
            shader.setFloatUniform("colere", colere)
            shader.setFloatUniform("tristesse", tristesse)
            shader.setFloatUniform("joie", joie)
            shader.setFloatUniform("calme", calme)
            shader.setFloatUniform("fatigue", fatigue)

            val brush = ShaderBrush(shader)
            onDrawBehind {
                drawRect(brush)
            }
        }
    )
}
