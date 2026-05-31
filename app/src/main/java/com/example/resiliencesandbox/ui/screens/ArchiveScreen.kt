package com.example.resiliencesandbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resiliencesandbox.ui.GameViewModel
import com.example.resiliencesandbox.ui.components.ExpandableFolderCard

@Composable
fun ArchiveScreen(viewModel: GameViewModel) {
    val character by viewModel.characterState.collectAsState()
    val semanticCharacter by viewModel.semanticCharacterState.collectAsState()
    val locations by viewModel.locationsState.collectAsState()
    val npcs by viewModel.npcsState.collectAsState()
    val inventory by viewModel.inventoryState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "=== ARCHIVES ===",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Section Finances & Profil
        item {
            Text(
                text = "[ FINANCES & PROFIL ]",
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ExpandableFolderCard(title = "État Général") {
                Column {
                    Text(
                        text = "Fonds disponibles : ${character?.argent ?: 0} €",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Énergie : ${semanticCharacter?.energie ?: "Inconnu"}",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Fatigue : ${semanticCharacter?.fatigue ?: "Inconnu"}",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "État psychologique : ${semanticCharacter?.peur ?: "Inconnu"}",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Toxicité : ${semanticCharacter?.toxicite ?: "Inconnu"}",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Inventaire Matériel
        item {
            Text(
                text = "[ INVENTAIRE MATÉRIEL ]",
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (inventory.isEmpty()) {
            item {
                Text(
                    text = "Sac vide",
                    color = Color.DarkGray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        } else {
            items(inventory) { item ->
                val semanticQuantity = when {
                    item.quantity == 1 -> ""
                    item.quantity in 2..4 -> "Quelques "
                    else -> "Un tas de "
                }
                val prefix = if (semanticQuantity.isEmpty()) item.name else "$semanticQuantity${item.name}"
                
                ExpandableFolderCard(title = "$prefix (${item.condition})") {
                    Text(
                        text = item.description,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Dossiers des Lieux
        item {
            Text(
                text = "[ DOSSIERS DES LIEUX ]",
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        if (locations.isEmpty()) {
            item {
                Text(
                    text = "Aucun lieu enregistré.",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        } else {
            items(locations) { location ->
                ExpandableFolderCard(title = location.name) {
                    Column {
                        Text(
                            text = "Description : ${location.description}",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Nombre de visites : ${location.visitCount}",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Registre des Relations
        item {
            Text(
                text = "[ REGISTRE DES RELATIONS ]",
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (npcs.isEmpty()) {
            item {
                Text(
                    text = "Aucun contact enregistré.",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            items(npcs) { npc ->
                ExpandableFolderCard(title = npc.name) {
                    Column {
                        Text(
                            text = "Statut : ${viewModel.getSemanticRelation(npc.relationScore)}",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Notes : ${npc.memoryNotes}",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
