package com.example.resiliencesandbox.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.resiliencesandbox.data.CharacterRepository
import com.example.resiliencesandbox.data.local.AppDatabase

class RoutineTickWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // Helper pour garantir que la valeur reste entre 0 et 100
    private fun Int.clampTo100(): Int = this.coerceIn(0, 100)

    override suspend fun doWork(): Result {
        // 1. Initialisation manuelle du Repository (sans Hilt)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CharacterRepository(database.characterDao(), database.eventLogDao())

        // 2. Récupération de l'état
        val character = repository.getCharacterState() ?: return Result.success()

        // 3. Logique du Tick : baisse d'énergie par défaut due au temps qui passe
        var newEnergie = character.energie - 2
        var newArgent = character.argent
        var newPeur = character.peur
        var newJoie = character.joie
        var newFatigue = character.fatigue
        
        val logMessage: String

        // 4. Modificateurs mathématiques liés à la postureActuelle
        when (character.postureActuelle) {
            "Prudent" -> {
                newEnergie += 5
                newPeur -= 2
                logMessage = "Routine : J'ai passé les dernières heures à me reposer prudemment."
            }
            "Impulsif" -> {
                newArgent -= 5
                newJoie += 5
                logMessage = "Routine : Je n'ai pas pu m'empêcher d'agir sur un coup de tête."
            }
            "Ambitieux" -> {
                newEnergie -= 5
                newPeur -= 2
                logMessage = "Routine : J'ai travaillé d'arrache-pied pour avancer vers mes objectifs."
            }
            "Paranoïaque" -> {
                newFatigue += 5
                newPeur += 2
                logMessage = "Routine : Je suis resté sur le qui-vive, guettant la moindre menace."
            }
            "Altruiste" -> {
                newEnergie -= 2
                newJoie += 2
                logMessage = "Routine : J'ai consacré mon temps à aider mon entourage."
            }
            "Analytique" -> {
                newFatigue += 5
                newJoie -= 2
                logMessage = "Routine : J'ai froidement analysé chaque détail de la situation en cours."
            }
            "Hédoniste" -> {
                newArgent -= 5
                newFatigue -= 5
                newJoie += 5
                logMessage = "Routine : J'ai profité de l'instant présent sans me soucier des conséquences."
            }
            else -> {
                logMessage = "Routine : Le temps s'est écoulé sans évènement notable."
            }
        }

        // 5. Clamper (restreindre) strictement les jauges de stats et émotions entre 0 et 100
        val updatedCharacter = character.copy(
            energie = newEnergie.clampTo100(),
            argent = maxOf(0, newArgent), // L'argent n'est pas limité à 100, mais on évite le négatif
            peur = newPeur.clampTo100(),
            joie = newJoie.clampTo100(),
            fatigue = newFatigue.clampTo100()
        )

        // 6. Sauvegarde et Historisation (isRoutineTick = true)
        repository.updateCharacter(updatedCharacter)
        repository.addEventLog(
            timestamp = System.currentTimeMillis(),
            description = logMessage,
            isRoutineTick = true
        )

        return Result.success()
    }
}
