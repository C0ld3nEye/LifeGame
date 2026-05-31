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
    val timeString by viewModel.timeString.collectAsState()
    val agenda by viewModel.agendaState.collectAsState()
    val affections by viewModel.affectionsState.collectAsState()
    val skills by viewModel.skillsState.collectAsState()

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
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Heure locale : $timeString",
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Section Agenda
        item {
            Text(
                text = "[ AGENDA ET ÉCHÉANCES ]",
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (agenda.isEmpty()) {
            item {
                Text(
                    text = "Aucune tâche en attente.",
                    color = Color.DarkGray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        } else {
            items(agenda) { event ->
                ExpandableFolderCard(title = "${event.title} (${event.echeance})") {
                    Text(
                        text = event.description,
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
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "--- ÉTAT BIOLOGIQUE ---",
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (affections.isEmpty()) {
                        Text(
                            text = "Organisme sain",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        affections.forEach { affection ->
                            val color = when (affection.type) {
                                "Blessure" -> Color.Red
                                "Maladie" -> Color(0xFF827717)
                                else -> Color(0xFFFFA500)
                            }
                            Text(
                                text = "- ${affection.nom} (${affection.type}): ${affection.description}",
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Compétences Acquises
        item {
            Text(
                text = "[ COMPÉTENCES ACQUISES ]",
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (skills.isEmpty()) {
            item {
                Text(
                    text = "Aucune compétence mémorisée.",
                    color = Color.DarkGray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        } else {
            items(skills) { skill ->
                val level = skill.level
                val filledBlocks = level / 10
                val emptyBlocks = 10 - filledBlocks
                val progressBar = "[" + "=".repeat(filledBlocks) + " ".repeat(emptyBlocks) + "]"
                
                Text(
                    text = "${skill.name} $progressBar $level/100",
                    color = Color(0xFF4DB6AC), // Teal
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
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
                ExpandableFolderCard(title = "${item.name} (${item.condition}) x${item.quantity}") {
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
                            text = "Sentiment : ${location.sentiment}",
                            color = Color.Cyan,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Passif : ${location.passif}",
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
                val relationColor = when (npc.relationScore) {
                    in 0..20 -> Color.Red
                    in 21..40 -> Color(0xFFFFA500)
                    in 41..60 -> Color.Gray
                    in 61..80 -> Color.White
                    else -> Color.Cyan
                }
                ExpandableFolderCard(title = npc.name) {
                    Column {
                        Text(
                            text = "Statut : ${viewModel.getSemanticRelation(npc.relationScore)}",
                            color = relationColor,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = npc.memoryNotes,
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
