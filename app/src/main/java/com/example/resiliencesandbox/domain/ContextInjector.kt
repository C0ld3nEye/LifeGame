package com.example.resiliencesandbox.domain

import com.example.resiliencesandbox.data.CharacterRepository
import com.example.resiliencesandbox.data.local.entity.CharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContextInjector(
    private val repository: CharacterRepository
) {

    suspend fun buildOmniscientPrompt(userQuery: String): String = withContext(Dispatchers.IO) {
        // 1. Récupération de l'état actuel ou d'un état par défaut
        val character = repository.getCharacterState() ?: CharacterEntity(
            id = 1,
            argent = 0,
            energie = 100,
            postureActuelle = "Inconnue",
            physical = 0, social = 0, intellect = 0, survival = 0, finance = 0, willpower = 0, creativity = 0,
            peur = 0, colere = 0, tristesse = 0, joie = 0, calme = 0, fatigue = 0
        )

        // 2. Récupération des 3 derniers évènements (ils arrivent triés du plus récent au plus ancien via le DAO)
        // On les inverse (reversed) pour les avoir du plus ancien au plus récent chronologiquement.
        val recentLogs = repository.getRecentEventLogs(3).reversed()

        // 3. Formatage de la mémoire récente en texte
        val logsHistoriques = if (recentLogs.isEmpty()) {
            "Aucun souvenir récent."
        } else {
            recentLogs.joinToString(separator = "\n") { log ->
                "- ${log.descriptionText}"
            }
        }

        // 4. Injection et formatage dans le template strict demandé
        """
        <start_of_turn>user
        [SYSTÈME]
        Tu es le Maître de Jeu impitoyable d'un jeu de survie textuel. 
        Règle 1 : Ton ton est sec, brutal et ultra-factuel. Aucune métaphore, aucun lyrisme. Décris la scène en 3 phrases maximum.
        Règle 2 : Tu dois TOUJOURS terminer ton texte par un choix d'action clair pour le joueur.
        Règle 3 : À la toute fin de ta réponse, tu DOIS obligatoirement générer un bloc de données technique encadré par les balises <DATA> et </DATA>. Ce bloc contiendra un objet JSON représentant l'impact de la situation sur le joueur.

        [ÉTAT INTERNE ET STATISTIQUES]
        Posture: ${character.postureActuelle} | Argent: ${character.argent}€ | Énergie: ${character.energie}/100
        Compétences: Physical(${character.physical}), Social(${character.social}), Intellect(${character.intellect}), Survival(${character.survival}), Finance(${character.finance}), Willpower(${character.willpower}), Creativity(${character.creativity})
        Émotions dominantes: Peur(${character.peur}), Colère(${character.colere}), Tristesse(${character.tristesse}), Joie(${character.joie}), Calme(${character.calme}), Fatigue(${character.fatigue})

        [MÉMOIRE RÉCENTE]
        $logsHistoriques

        QUESTION DU JOUEUR : "$userQuery"
        <end_of_turn>
        <start_of_turn>model
        """.trimIndent()
    }
}
