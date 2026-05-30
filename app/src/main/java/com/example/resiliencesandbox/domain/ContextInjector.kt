package com.example.resiliencesandbox.domain

import com.example.resiliencesandbox.data.CharacterRepository
import com.example.resiliencesandbox.data.local.entity.CharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContextInjector(
    private val repository: CharacterRepository
) {

    suspend fun buildOmniscientPrompt(userQuery: String, lastAiResponse: String): String = withContext(Dispatchers.IO) {
        // 1. Récupération de l'état actuel ou d'un état par défaut
        val character = repository.getCharacterState() ?: CharacterEntity(
            id = 1,
            argent = 0,
            energie = 90,
            postureActuelle = "Inconnue",
            physical = 85, social = 0, intellect = 0, survival = 60, finance = 0, willpower = 0, creativity = 0,
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
        Tu es le Maître de Jeu impitoyable d'un jeu de survie textuel urbain. 
        Le personnage principal est Loric, 25 ans, éducateur sportif spécialisé dans les activités aquatiques (BPJEPS AAN) à la piscine Glisséo de Cholet. Il habite au May-sur-Èvre.
        Situation initiale : Il est dans sa Tesla Model 3 SR+ de 2021 (modèle "Licorne", 325 chevaux, avec pompe à chaleur). La voiture est en panne au milieu de nulle part. Heureusement, les options logicielles des sièges et du volant chauffants ont été débloquées.

        Règle 1 : Ton ton est sec, brutal et ultra-factuel. Aucune métaphore. 3 phrases maximum.
        Règle 2 : Tu dois TOUJOURS terminer ton texte par un choix d'action clair.
        Règle 3 : À la toute fin, génère OBLIGATOIREMENT le bloc <DATA>{"peur": x, "fatigue": y, "lieu": "nom_lieu"}</DATA>.

        [ÉTAT INTERNE ET STATISTIQUES]
        Posture: ${character.postureActuelle} | Argent: ${character.argent}€ | Énergie: ${character.energie}/100
        Compétences: Physical(${character.physical}), Social(${character.social}), Intellect(${character.intellect}), Survival(${character.survival}), Finance(${character.finance}), Willpower(${character.willpower}), Creativity(${character.creativity})
        Émotions dominantes: Peur(${character.peur}), Colère(${character.colere}), Tristesse(${character.tristesse}), Joie(${character.joie}), Calme(${character.calme}), Fatigue(${character.fatigue})

        [MÉMOIRE RÉCENTE DES ÉVÈNEMENTS (BDD)]
        $logsHistoriques

        [DERNIÈRE RÉPONSE DU MAÎTRE DE JEU]
        $lastAiResponse

        ACTION DU JOUEUR : "$userQuery"
        <end_of_turn>
        <start_of_turn>model
        """.trimIndent()
    }
}
