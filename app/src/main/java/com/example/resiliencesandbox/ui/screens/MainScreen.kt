package com.example.resiliencesandbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resiliencesandbox.ui.GameViewModel

@Composable
fun MainScreen(viewModel: GameViewModel) {
    var currentTab by remember { mutableStateOf(0) } // 0 = Action (Terminal), 1 = Archives

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[ ACTION ]",
                    color = if (currentTab == 0) Color.White else Color.DarkGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable { currentTab = 0 }
                )
                Text(
                    text = "[ ARCHIVES ]",
                    color = if (currentTab == 1) Color.White else Color.DarkGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable { currentTab = 1 }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(Color.Black)
        ) {
            // Pour s'assurer que le streaming LLM ne se coupe pas et que l'on ne perde pas
            // l'état de défilement, on laisse GameScreen dans l'arbre de composition.
            GameScreen(viewModel = viewModel)
            
            // Si on est sur l'onglet Archives, on l'affiche par-dessus (Overlay)
            if (currentTab == 1) {
                ArchiveScreen(viewModel = viewModel)
            }
        }
    }
}
