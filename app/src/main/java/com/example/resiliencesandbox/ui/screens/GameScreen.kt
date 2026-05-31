package com.example.resiliencesandbox.ui.screens

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
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
    uv.x = abs(uv.x - 0.5); // Symétrie parfaite
    
    // Coordonnées pour le bruit
    float2 p = uv * 3.0;
    float t = time * 0.2;
    
    // Création de couches de bruit superposées
    float noiseVal = fbm(p + t);
    
    // Forme de base (plus "étalée" qu'un simple cercle)
    float dist = length(uv - float2(0.0, 0.4));
    
    // Masque organique : la combinaison de la distance et du bruit fractal
    // Le 'ink' est plus souple pour laisser apparaître les bords dentelés
    float ink = smoothstep(0.42, 0.40, dist + (noiseVal * 0.4));
    
    // Couleur de base argentée
    float3 color = float3(0.6, 0.6, 0.65);
    
    // Teintes émotionnelles (mélange progressif)
    color = mix(color, float3(0.8, 0.2, 0.2), colere * ink);
    color = mix(color, float3(0.2, 0.3, 0.9), tristesse * ink);
    color = mix(color, float3(0.9, 0.9, 0.3), joie * ink);
    
    return half4(color * ink, ink);
}
"""

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val characterState by viewModel.characterState.collectAsState()
    val narrativeText by viewModel.narrativeText.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    val inventoryState by viewModel.inventoryState.collectAsState()
    val npcsState by viewModel.npcsState.collectAsState()
    val currentLocationName by viewModel.currentLocationName.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }

    var inputText by remember { mutableStateOf("") }

    // Rorschach values mapping: (val / 100f) pour obtenir des floats entre 0.0 et 1.0
    val peur = (characterState?.peur ?: 0) / 100f
    val colere = (characterState?.colere ?: 0) / 100f
    val tristesse = (characterState?.tristesse ?: 0) / 100f
    val joie = (characterState?.joie ?: 0) / 100f
    val calme = (characterState?.calme ?: 0) / 100f
    val fatigue = (characterState?.fatigue ?: 0) / 100f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Le fond de l'application est noir profond
    ) {
        // 1. RorschachCanvas en plein écran
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RorschachCanvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showBottomSheet = true },
                peur = peur,
                colere = colere,
                tristesse = tristesse,
                joie = joie,
                calme = calme,
                fatigue = fatigue
            )
        } else {
            // Fallback pour assurer la compilation sur SDK < 33 si existant
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
                    .clickable { showBottomSheet = true }
            )
        }

        // 2. Notifications flottantes (En haut à droite)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .padding(top = 24.dp), // Pour éviter la status bar
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notifications.forEach { notif ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E1E1E), RectangleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(notif.text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // 3. UI d'interaction (En bas de l'écran)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Narration textuelle (avec flou si isThinking et ombre portée pour la lisibilité)
            val scrollState = rememberScrollState()

            LaunchedEffect(narrativeText) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }

            Text(
                text = narrativeText.ifEmpty { "La noirceur t'enveloppe. Ton esprit dérive..." },
                color = Color.LightGray,
                textAlign = TextAlign.Justify,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(2f, 2f),
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .heightIn(max = 250.dp)
                    .verticalScroll(scrollState)
            )

            // Bloc input "Terminal"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)
                
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.LightGray,
                        unfocusedTextColor = Color.LightGray,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.LightGray
                    ),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                    placeholder = { Text("Entrez une action...", color = Color.DarkGray, fontFamily = FontFamily.Monospace) },
                    leadingIcon = { Text("> ", color = Color.LightGray, fontFamily = FontFamily.Monospace) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isThinking) {
                                viewModel.submitPlayerAction(inputText)
                                inputText = ""
                            }
                        }
                    )
                )
            }
        }
    }

    if (showBottomSheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = Color(0xFF121212),
            contentColor = Color.LightGray,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: État Psychologique & Physique
                Text("ÉTAT PSYCHOLOGIQUE & PHYSIQUE", style = MaterialTheme.typography.titleMedium, color = Color.White, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                val semanticState by viewModel.semanticCharacterState.collectAsState()
                Text("Énergie : ${semanticState?.energie ?: "Inconnu"}", fontFamily = FontFamily.Monospace)
                Text("Fatigue : ${semanticState?.fatigue ?: "Inconnu"}", fontFamily = FontFamily.Monospace)
                Text("Peur    : ${semanticState?.peur ?: "Inconnu"}", fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Objets Possédés
                Text("OBJETS POSSÉDÉS", style = MaterialTheme.typography.titleMedium, color = Color.White, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                if (inventoryState.isEmpty()) {
                    Text("Aucune donnée", color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                } else {
                    inventoryState.forEach { item ->
                        val prefix = when {
                            item.quantity == 1 -> ""
                            item.quantity in 2..4 -> "Quelques "
                            else -> "Plusieurs "
                        }
                        Text("- $prefix${item.name}", fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Présences & Lieux
                Text("PRÉSENCES & LIEUX", style = MaterialTheme.typography.titleMedium, color = Color.White, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Lieu actuel : $currentLocationName", fontFamily = FontFamily.Monospace)
                if (npcsState.isEmpty()) {
                    Text("Aucune donnée", color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                } else {
                    npcsState.forEach { npc ->
                        Text("- ${npc.name} (${npc.memoryNotes})", fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
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
        var lastFrameTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                val delta = (frameTime - lastFrameTime) / 1_000_000_000f
                time += delta
                lastFrameTime = frameTime
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
