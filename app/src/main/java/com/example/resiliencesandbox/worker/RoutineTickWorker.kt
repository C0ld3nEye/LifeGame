package com.example.resiliencesandbox.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.resiliencesandbox.data.CharacterRepository
import com.example.resiliencesandbox.data.local.AppDatabase
import com.example.resiliencesandbox.domain.ContextInjector
import com.example.resiliencesandbox.engine.LiteRtManager
import com.example.resiliencesandbox.ui.TurnData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class RoutineTickWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private fun Int.clampTo100(): Int = this.coerceIn(0, 100)

    override suspend fun doWork(): Result {
        val liteRtManager = LiteRtManager(applicationContext)
        
        try {
            // 1. Initialisation manuelle
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = CharacterRepository(database.characterDao(), database.eventLogDao())
            val contextInjector = ContextInjector(
                repository,
                database.locationDao(),
                database.npcDao(),
                database.inventoryDao(),
                database.affectionDao(),
                database.skillDao(),
                database.agendaDao()
            )

            val character = repository.getCharacterState() ?: return Result.success()

            // 2. Chargement du Modèle
            val externalFiles = applicationContext.getExternalFilesDir(null)
            val modelFile = externalFiles?.listFiles()?.firstOrNull {
                it.name.endsWith(".litertlm") || it.name.endsWith(".bin") || it.name.contains("gemma")
            } ?: File(externalFiles, "gemma.litertlm")

            if (!modelFile.exists()) {
                Log.w("RoutineTickWorker", "Modèle IA introuvable pour la simulation hors-ligne.")
                return Result.success()
            }

            liteRtManager.initializeModel(modelFile.absolutePath)

            // 3. Calcul du temps écoulé (Stricte proportion 1:1, conversion en heures)
            val lastInteractionEvent = repository.getRecentEventLogs(1).firstOrNull()
            val lastTime = lastInteractionEvent?.timestamp ?: character.gameStartTime
            val elapsedMillisRealTime = System.currentTimeMillis() - lastTime
            val elapsedHours = elapsedMillisRealTime / (1000 * 60 * 60)
            
            // Mise à jour de l'horloge in-game
            val newGameTime = character.gameTimeMinutes + (elapsedMillisRealTime / (1000 * 60))

            // 3.5 Dégénérescence Biologique Active
            var baseEnergie = character.energie
            var baseFatigue = character.fatigue
            var lastDegenerationMinute = character.lastDegenerationMinute
            val hoursElapsedForDegeneration = (newGameTime - lastDegenerationMinute) / 60
            
            if (hoursElapsedForDegeneration >= 1) {
                val activeAffections = database.affectionDao().getAllAffections()
                val malusCount = activeAffections.size * hoursElapsedForDegeneration.toInt()
                if (malusCount > 0) {
                    baseEnergie = (baseEnergie - (3 * malusCount)).clampTo100()
                    baseFatigue = (baseFatigue + (2 * malusCount)).clampTo100()
                }
                lastDegenerationMinute += hoursElapsedForDegeneration * 60
            }

            // Dégénérescence des compétences (-1 pt toutes les 96h, plancher à 10)
            val allSkills = database.skillDao().getAllSkills()
            allSkills.forEach { skill ->
                val hoursSinceUsed = (newGameTime - skill.lastUsedMinute) / 60
                if (hoursSinceUsed >= 96) {
                    val dropInstances = (hoursSinceUsed / 96).toInt()
                    val newLevel = (skill.level - dropInstances).coerceAtLeast(10)
                    if (newLevel != skill.level) {
                        val newLastUsedMinute = skill.lastUsedMinute + (dropInstances * 96 * 60)
                        database.skillDao().insertSkill(skill.copy(level = newLevel, lastUsedMinute = newLastUsedMinute))
                    }
                }
            }

            // 4. Exécution du LLM
            val prompt = contextInjector.buildOmniscientPrompt(
                "[SIMULATION HORS-LIGNE] Le joueur est absent depuis $elapsedHours heures (temps in-game). Que se passe-t-il dans le monde ? N'invente pas d'actions physiques pour le joueur. Concentre-toi sur les PNJ et l'agenda.",
                "",
                null,
                false
            )

            val fullResponseBuilder = StringBuilder()
            liteRtManager.generateResponseStream(prompt).collect { chunk ->
                fullResponseBuilder.append(chunk)
            }

            // 5. Parsing
            val fullResponse = fullResponseBuilder.toString()
            val regex = "<DATA>(.*?)</DATA>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matchResult = regex.find(fullResponse)

            if (matchResult != null) {
                val cleanJson = matchResult.groupValues[1].trim()
                val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
                val turnData = jsonParser.decodeFromString<TurnData>(cleanJson)

                val newPeur = turnData.peur?.let { (character.peur + it).clampTo100() } ?: character.peur
                val newFatigue = turnData.fatigue?.let { (baseFatigue + it).clampTo100() } ?: baseFatigue
                val newEnergie = turnData.energie?.let { (baseEnergie + it).clampTo100() } ?: baseEnergie
                val newToxicite = turnData.toxicite?.let { (character.toxicite + it).clampTo100() } ?: character.toxicite

                val updatedCharacter = character.copy(
                    peur = newPeur,
                    fatigue = newFatigue,
                    energie = newEnergie,
                    toxicite = newToxicite,
                    gameTimeMinutes = newGameTime,
                    lastDegenerationMinute = lastDegenerationMinute
                )

                repository.updateCharacter(updatedCharacter)
                
                // Traitement des affections générées hors-ligne
                turnData.nouvelles_affections?.forEach { affection ->
                    val safeName = affection.nom.trim()
                    if (safeName.isNotEmpty()) {
                        database.affectionDao().insertAffection(
                            com.example.resiliencesandbox.data.local.entity.AffectionEntity(
                                id = safeName,
                                nom = safeName,
                                type = affection.type,
                                description = affection.description
                            )
                        )
                    }
                }
                turnData.affections_soignees?.forEach { affectionName ->
                    val safeName = affectionName.trim()
                    if (safeName.isNotEmpty()) {
                        database.affectionDao().deleteAffectionByName(safeName)
                    }
                }

                turnData.competences_ameliorees?.forEach { (nomCompetence, gain) ->
                    val safeName = nomCompetence.trim()
                    if (safeName.isNotEmpty()) {
                        val existingSkill = database.skillDao().getSkillByName(safeName)
                        if (existingSkill != null) {
                            val newLevel = (existingSkill.level + gain).coerceAtMost(100)
                            database.skillDao().insertSkill(existingSkill.copy(level = newLevel, lastUsedMinute = newGameTime))
                        } else {
                            val newLevel = gain.coerceAtMost(100)
                            database.skillDao().insertSkill(com.example.resiliencesandbox.data.local.entity.SkillEntity(safeName, newLevel, newGameTime))
                        }
                    }
                }

                val logMessage = turnData.resume_action ?: "Le temps passe."
                repository.addEventLog(
                    timestamp = System.currentTimeMillis(),
                    description = logMessage,
                    isRoutineTick = true
                )

                // 6. Push Notification Organique
                turnData.alerte_push?.let { alerte ->
                    if (alerte.isNotBlank()) {
                        sendSystemNotification("Le Monde Bouge", alerte)
                    }
                }
            } else {
                repository.updateCharacter(character.copy(
                    energie = baseEnergie,
                    fatigue = baseFatigue,
                    gameTimeMinutes = newGameTime,
                    lastDegenerationMinute = lastDegenerationMinute
                ))
            }
            
        } catch (e: Exception) {
            Log.e("RoutineTickWorker", "Erreur lors de la simulation hors-ligne", e)
        } finally {
            // 7. PURGE OBLIGATOIRE DE LA RAM
            liteRtManager.close()
        }

        return Result.success()
    }

    private fun sendSystemNotification(title: String, message: String) {
        val channelId = "task_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tâches en jeu",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(applicationContext).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            Log.e("RoutineTickWorker", "Permission manquante pour POST_NOTIFICATIONS", e)
        }
    }
}
