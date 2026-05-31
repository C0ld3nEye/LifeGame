package com.example.resiliencesandbox.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.resiliencesandbox.data.CharacterRepository
import com.example.resiliencesandbox.data.local.dao.LocationDao
import com.example.resiliencesandbox.data.local.dao.NpcDao
import com.example.resiliencesandbox.data.local.dao.InventoryDao
import com.example.resiliencesandbox.data.local.entity.CharacterEntity
import com.example.resiliencesandbox.data.local.entity.LocationEntity
import com.example.resiliencesandbox.data.local.entity.NpcEntity
import com.example.resiliencesandbox.data.local.entity.InventoryEntity
import com.example.resiliencesandbox.domain.ContextInjector
import com.example.resiliencesandbox.engine.LiteRtManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.random.Random
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

data class NotificationData(val id: Long, val text: String)

@Serializable
data class ItemData(
    val nom: String,
    val etat: String,
    val description: String
)

@Serializable
data class NpcUpdate(
    val nom: String,
    val evolution_relation: Int,
    val nouveau_souvenir: String
)

@Serializable
data class AgendaUpdate(
    val titre: String,
    val description: String,
    val echeance: String
)

@Serializable
data class Affection(
    val nom: String,
    val type: String,
    val description: String
)

@Serializable
data class TurnData(
    val peur: Int? = null,
    val fatigue: Int? = null,
    val energie: Int? = null,
    val toxicite: Int? = null,
    val lieu: String? = null,
    val description_lieu: String? = null,
    val sentiment_lieu: String? = null,
    val passif_lieu: String? = null,
    val meteo: String? = null,
    val resume_action: String? = null,
    val consequence_immediate: String? = null,
    val alerte_push: String? = null,
    val nouveaux_pnj: List<String>? = null,
    val nouveaux_objets: List<ItemData>? = null,
    val nouveaux_evenements_agenda: List<AgendaUpdate>? = null,
    val objets_consommes: List<String>? = null,
    val pnj_interagis: List<NpcUpdate>? = null,
    val duree_tache_minutes: Int? = null,
    val nom_tache: String? = null,
    val nouvelles_affections: List<Affection>? = null,
    val affections_soignees: List<String>? = null,
    val competences_ameliorees: Map<String, Int>? = null
)

data class SemanticCharacterState(
    val energie: String,
    val fatigue: String,
    val peur: String,
    val toxicite: String
)

data class TaskDeltas(
    val peur: Int,
    val fatigue: Int,
    val energie: Int,
    val toxicite: Int
)

class GameViewModel(
    private val context: Context,
    private val repository: CharacterRepository,
    private val contextInjector: ContextInjector,
    private val liteRtManager: LiteRtManager,
    private val locationDao: LocationDao,
    private val npcDao: NpcDao,
    private val inventoryDao: InventoryDao,
    private val agendaDao: com.example.resiliencesandbox.data.local.dao.AgendaDao,
    private val affectionDao: com.example.resiliencesandbox.data.local.dao.AffectionDao,
    private val skillDao: com.example.resiliencesandbox.data.local.dao.SkillDao
) : ViewModel() {

    // 1. Observation en continu de l'état du personnage (Base de Données -> UI)
    val characterState: StateFlow<CharacterEntity?> = repository.characterFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val semanticCharacterState: StateFlow<SemanticCharacterState?> = repository.characterFlow
        .map { character ->
            if (character == null) null
            else SemanticCharacterState(
                energie = when {
                    character.fatigue >= 90 -> "Épuisement nerveux"
                    character.fatigue >= 60 -> "Lourdeur physique"
                    character.energie in 76..100 -> "Réserves pleines"
                    character.energie in 51..75 -> "Baisse de régime"
                    character.energie in 26..50 -> "Affaibli (faim/carences)"
                    character.energie in 1..25 -> "Épuisement calorique"
                    else -> "Inconscient"
                },
                fatigue = when (character.fatigue) {
                    in 0..25 -> "Esprit clair"
                    in 26..60 -> "Lourdeur naissante"
                    in 61..90 -> "Épuisement"
                    else -> "Au bord du malaise"
                },
                peur = when (character.peur) {
                    in 0..20 -> "Serein"
                    in 21..50 -> "Inquiet"
                    in 51..80 -> "Angoissé"
                    else -> "Paniqué"
                },
                toxicite = when (character.toxicite) {
                    in 0..10 -> "Organisme sain"
                    in 11..40 -> "Carences légères"
                    in 41..70 -> "Dette métabolique lourde"
                    else -> "Défaillance systémique"
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 2. StateFlow pour le texte narratif de l'IA (initialisé à vide)
    private val _narrativeText = MutableStateFlow("")
    val narrativeText: StateFlow<String> = _narrativeText.asStateFlow()

    // HUD Principaux
    private val _currentWeather = MutableStateFlow("Inconnu")
    val currentWeather: StateFlow<String> = _currentWeather.asStateFlow()

    private val _actionSummary = MutableStateFlow("En attente...")
    val actionSummary: StateFlow<String> = _actionSummary.asStateFlow()

    private val _timeString = MutableStateFlow("Jour 1, 00:00")
    val timeString: StateFlow<String> = _timeString.asStateFlow()

    private var taskEndTime: Long? = null
    private var taskTotalDuration: Long? = null
    private var currentTaskName: String? = null
    
    private val _currentTaskDisplay = MutableStateFlow<String?>(null)
    val currentTaskDisplay: StateFlow<String?> = _currentTaskDisplay.asStateFlow()

    private val _consequenceState = MutableStateFlow<String?>(null)
    val consequenceState: StateFlow<String?> = _consequenceState.asStateFlow()

    private var taskTimerJob: Job? = null

    // 3. StateFlow pour indiquer si l'IA réfléchit (temps de chargement)
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    // 4. Mémoire à court terme pour éviter l'amnésie conversationnelle immédiate
    private var lastAiResponse: String = ""
    private var storyLog: String = ""

    // 5. Exposition des données du monde (Panneau de Conscience)
    private val _inventoryState = MutableStateFlow<List<InventoryEntity>>(emptyList())
    val inventoryState: StateFlow<List<InventoryEntity>> = _inventoryState.asStateFlow()

    private val _npcsState = MutableStateFlow<List<NpcEntity>>(emptyList())
    val npcsState: StateFlow<List<NpcEntity>> = _npcsState.asStateFlow()

    private val _currentLocationName = MutableStateFlow("Inconnu")
    val currentLocationName: StateFlow<String> = _currentLocationName.asStateFlow()

    private val _locationsState = MutableStateFlow<List<LocationEntity>>(emptyList())
    val locationsState: StateFlow<List<LocationEntity>> = _locationsState.asStateFlow()

    val agendaState: StateFlow<List<com.example.resiliencesandbox.data.local.entity.AgendaEntity>> = agendaDao.getAllAgenda()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val affectionsState: StateFlow<List<com.example.resiliencesandbox.data.local.entity.AffectionEntity>> = affectionDao.getAllAffectionsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val skillsState: StateFlow<List<com.example.resiliencesandbox.data.local.entity.SkillEntity>> = skillDao.getAllSkillsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var idleTimerJob: Job? = null

    private fun stopIdleTimer() {
        idleTimerJob?.cancel()
    }

    private fun resetIdleTimer() {
        idleTimerJob?.cancel()
        idleTimerJob = viewModelScope.launch {
            delay(180_000L) // 3 minutes d'inactivité
            // Force une action contextuelle silencieuse
            submitPlayerAction("[INACTIVITÉ] Le joueur n'agit pas. Le temps passe. Fais avancer l'histoire de force. Déclenche un événement extérieur (bruit imprévu, appel, PNJ qui agit) ou une urgence physique (faim, froid) qui le sort de sa torpeur. Le monde doit vivre sans lui.", isHidden = true)
        }
    }

    init {
        viewModelScope.launch {
            if (repository.getCharacterState() == null) {
                repository.saveCharacterState(
                    CharacterEntity(
                        id = 1,
                        argent = 0,
                        energie = 100,
                        postureActuelle = "Inconnue",
                        gameTimeMinutes = 0L,
                        physical = 50, social = 50, intellect = 50, survival = 50, finance = 50, willpower = 50, creativity = 50,
                        peur = 0, colere = 0, tristesse = 0, joie = 0, calme = 0, fatigue = 0, toxicite = 0,
                        obsession = "Trouver un endroit sûr pour se reposer."
                    )
                )
                storyLog = "Qui es-tu ?"
                lastAiResponse = "Qui es-tu ?"
            } else {
                val recentLogs = repository.getRecentEventLogs(5).reversed()
                if (recentLogs.isNotEmpty()) {
                    val recap = "Résumé de tes dernières actions :\n" + recentLogs.joinToString("\n") { "> ${it.descriptionText}" }
                    storyLog = recap + "\n\nDe retour à la réalité. Que fais-tu ?"
                    lastAiResponse = "De retour à la réalité. Que fais-tu ?"
                } else {
                    storyLog = "De retour à la réalité. Que fais-tu ?"
                    lastAiResponse = "De retour à la réalité. Que fais-tu ?"
                }
            }
            _narrativeText.value = storyLog
            refreshWorldData()
            triggerWakeUpIfNeeded()
        }

        // Horloge dynamique
        viewModelScope.launch {
            while (true) {
                val currentState = repository.getCharacterState()
                if (currentState != null) {
                    val elapsedMillis = System.currentTimeMillis() - currentState.gameStartTime
                    val totalMinutes = elapsedMillis / (1000 * 60)
                    val totalHours = totalMinutes / 60
                    val totalDays = totalHours / 32
                    val currentYear = (totalDays / 360) + 1
                    val currentMonth = ((totalDays % 360) / 30) + 1
                    val currentDay = (totalDays % 30) + 1
                    val currentHour = totalHours % 32
                    val currentMinute = totalMinutes % 60
                    _timeString.value = String.format("Année %d, Mois %d, Jour %d - %02d:%02d", currentYear, currentMonth, currentDay, currentHour, currentMinute)
                }
                delay(1000L)
            }
        }

        resetIdleTimer()
    }

    private fun triggerWakeUpIfNeeded() {
        viewModelScope.launch {
            val logs = repository.getRecentEventLogs(1)
            if (logs.isNotEmpty() && logs.first().isRoutineTick) {
                // Le joueur revient après l'exécution du Worker
                submitPlayerAction("[REPRISE DE CONSCIENCE]", isWakeUp = true)
            }
        }
    }

    private fun refreshWorldData() {
        viewModelScope.launch {
            _inventoryState.value = inventoryDao.getAllInventory()
            _npcsState.value = npcDao.getAllNpcs()
            val locations = locationDao.getAllLocations()
            _locationsState.value = locations
            _currentLocationName.value = locationDao.getLastLocation()?.name ?: "Inconnu"
        }
    }

    /**
     * Traite l'action entrée par le joueur, construit le prompt omniscient,
     * l'envoie au modèle et met à jour la réponse narrativement.
     */
    fun submitPlayerAction(userText: String, isWakeUp: Boolean = false, isHidden: Boolean = false) {
        stopIdleTimer()
        _consequenceState.value = null
        
        var finalUserText = userText
        
        if (taskEndTime != null && !isWakeUp && !isHidden) {
            val total = taskTotalDuration ?: 1L
            val elapsed = total - (taskEndTime!! - System.currentTimeMillis())
            val ratio = (elapsed.toFloat() / total).coerceIn(0f, 1f)
            
            val ratioPercent = (ratio * 100).toInt()
            val taskNameStr = currentTaskName ?: "Inconnue"
            finalUserText = "[INTERRUPTION DE TÂCHE] Le joueur vient de stopper l'action '$taskNameStr' à $ratioPercent%. La nouvelle action immédiate est : \"$userText\""
            
            taskTimerJob?.cancel()
            taskEndTime = null
            taskTotalDuration = null
            currentTaskName = null
            _currentTaskDisplay.value = null
        }

        viewModelScope.launch {
            _isThinking.value = true

            try {
                // Vérification du Système de Crise
                val character = repository.getCharacterState()
                var crisisReason: String? = null
                if (character != null) {
                    if (character.energie <= 0) crisisReason = "Épuisement total de l'énergie"
                    else if (character.fatigue >= 100) crisisReason = "Fatigue extrême et effondrement physique"
                    else if (character.peur >= 100) crisisReason = "Crise de panique insurmontable"
                }

                // Ajout de l'action du joueur à l'historique
                if (userText.isNotBlank() && !isWakeUp && !isHidden) {
                    storyLog += "\n\n> $userText\n\n"
                }

                // Récupération de la donnée + formatage strict (Pont de données)
                val prompt = contextInjector.buildOmniscientPrompt(finalUserText, lastAiResponse, crisisReason, isWakeUp)

                // Exécution asynchrone du LLM en streaming
                _narrativeText.value = storyLog
                val fullResponseBuilder = java.lang.StringBuilder()
                var isDataBlockStarted = false
                
                liteRtManager.generateResponseStream(prompt).collect { chunk ->
                    fullResponseBuilder.append(chunk)
                    val currentText = fullResponseBuilder.toString()
                    
                    if (!isDataBlockStarted) {
                        if (currentText.contains("<DATA>")) {
                            isDataBlockStarted = true
                            val narrativePart = currentText.substringBefore("<DATA>")
                            _narrativeText.value = storyLog + narrativePart.trim()
                        } else {
                            _narrativeText.value = storyLog + currentText
                        }
                    }
                }
                
                val fullResponse = fullResponseBuilder.toString()
                
                // On garde l'intégralité (y compris le JSON) pour la mémoire de l'IA
                lastAiResponse = fullResponse.trim()
                
                // On n'affiche que la partie narrative dans l'historique visuel
                val visibleNarrative = if (!isDataBlockStarted) {
                    fullResponse.trim()
                } else {
                    fullResponse.substringBefore("<DATA>").trim()
                }
                storyLog += "\n\n$visibleNarrative"
                // On isole le JSON de manière ultra-résiliente avec une Regex
                val regex = "<DATA>(.*?)</DATA>".toRegex(RegexOption.DOT_MATCHES_ALL)
                val matchResult = regex.find(fullResponse)
                
                if (matchResult != null) {
                    // Remplacer les éventuelles erreurs de syntaxe du LLM (apostrophes au lieu de guillemets)
                    val cleanJson = matchResult.groupValues[1].trim()
                    
                    try {
                        val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
                        val turnData = jsonParser.decodeFromString<TurnData>(cleanJson)

                        // Mise à jour de l'état du personnage via ton repository ou tes MutableStateFlow
                        val deltas = updateCharacterStats(
                            peur = turnData.peur,
                            fatigue = turnData.fatigue,
                            toxicite = turnData.toxicite,
                            energie = turnData.energie,
                            lieu = turnData.lieu
                        )

                        val duree = turnData.duree_tache_minutes
                        if (duree != null && duree > 0) {
                            taskTotalDuration = duree * 60 * 1000L
                            taskEndTime = System.currentTimeMillis() + taskTotalDuration!!
                            val taskName = turnData.nom_tache ?: "Action en cours..."
                            currentTaskName = taskName
                            
                            taskTimerJob?.cancel()
                            taskTimerJob = viewModelScope.launch {
                                while (true) {
                                    val end = taskEndTime ?: break
                                    val now = System.currentTimeMillis()
                                    if (now >= end) break
                                    val remaining = end - now
                                    val remainingMin = remaining / (60 * 1000)
                                    val remainingSec = (remaining / 1000) % 60
                                    _currentTaskDisplay.value = "$taskName : ${String.format("%02d:%02d", remainingMin, remainingSec)} restantes"
                                    kotlinx.coroutines.delay(1000)
                                }
                                val finalEnd = taskEndTime
                                if (finalEnd != null && System.currentTimeMillis() >= finalEnd) {
                                    // Tâche terminée naturellement
                                    _currentTaskDisplay.value = null
                                    taskEndTime = null
                                    taskTotalDuration = null
                                    sendSystemNotification("Tâche terminée", "La tâche '$taskName' est arrivée à son terme.")
                                    submitPlayerAction("[TÂCHE TERMINÉE] La tâche '$taskName' est arrivée à son terme.", isWakeUp = true)
                                }
                            }
                        }

                        // 1. Enregistrement du Lieu
                        turnData.lieu?.let { newLieu ->
                            val safeLieu = newLieu.trim()
                            if (safeLieu.isNotEmpty()) {
                                _currentLocationName.value = safeLieu
                                val existing = locationDao.getLocationById(safeLieu)
                                if (existing == null) {
                                    locationDao.insertLocation(
                                        LocationEntity(
                                            id = safeLieu,
                                            name = safeLieu,
                                            description = turnData.description_lieu ?: "Découvert récemment.",
                                            sentiment = turnData.sentiment_lieu ?: "Inconnu",
                                            passif = turnData.passif_lieu ?: "Aucun souvenir particulier."
                                        )
                                    )
                                } else {
                                    val newSentiment = turnData.sentiment_lieu ?: existing.sentiment
                                    val newPassif = turnData.passif_lieu ?: existing.passif
                                    locationDao.insertLocation(
                                        existing.copy(sentiment = newSentiment, passif = newPassif)
                                    )
                                }
                            }
                        }

                        // 2. Enregistrement des Nouveaux PNJ
                        turnData.nouveaux_pnj?.forEach { npcName ->
                            val safeName = npcName.trim()
                            if (safeName.isNotEmpty()) {
                                val existing = npcDao.getNpcById(safeName)
                                if (existing == null) {
                                    npcDao.insertNpc(
                                        NpcEntity(
                                            id = safeName,
                                            name = safeName,
                                            relationScore = 0,
                                            memoryNotes = "Rencontré à : ${turnData.lieu ?: "Inconnu"}"
                                        )
                                    )
                                }
                            }
                        }

                        // 2.5 Interactions PNJ
                        turnData.pnj_interagis?.forEach { update ->
                            val safeName = update.nom.trim()
                            if (safeName.isNotEmpty()) {
                                val existing = npcDao.getNpcById(safeName)
                                if (existing == null) {
                                    npcDao.insertNpc(
                                        NpcEntity(
                                            id = safeName,
                                            name = safeName,
                                            relationScore = (50 + update.evolution_relation).coerceIn(0, 100),
                                            memoryNotes = update.nouveau_souvenir
                                        )
                                    )
                                } else {
                                    val newScore = (existing.relationScore + update.evolution_relation).coerceIn(0, 100)
                                    val newNotes = existing.memoryNotes + "\n- " + update.nouveau_souvenir
                                    npcDao.insertNpc(
                                        existing.copy(relationScore = newScore, memoryNotes = newNotes)
                                    )
                                }
                            }
                        }

                        // 3. Enregistrement des Nouveaux Objets (Inventaire)
                        turnData.nouveaux_objets?.forEach { item ->
                            val safeName = item.nom.trim()
                            val safeCondition = item.etat.trim()
                            if (safeName.isNotEmpty() && safeCondition.isNotEmpty()) {
                                val combinedId = "${safeName}_${safeCondition}"
                                val existing = inventoryDao.getInventoryItem(combinedId)
                                if (existing == null) {
                                    inventoryDao.insertInventoryItem(
                                        InventoryEntity(
                                            id = combinedId,
                                            name = safeName,
                                            condition = safeCondition,
                                            description = item.description.trim(),
                                            quantity = 1
                                        )
                                    )
                                } else {
                                    // S'il existe, on augmente la quantité
                                    inventoryDao.insertInventoryItem(
                                        existing.copy(quantity = existing.quantity + 1)
                                    )
                                }
                            }
                        }

                        // 3.5 Consommation d'objets
                        turnData.objets_consommes?.forEach { nomObjet ->
                            val safeName = nomObjet.trim()
                            if (safeName.isNotEmpty()) {
                                val existing = inventoryDao.getInventoryItemByName(safeName)
                                if (existing != null) {
                                    if (existing.quantity > 1) {
                                        inventoryDao.insertInventoryItem(existing.copy(quantity = existing.quantity - 1))
                                    } else {
                                        inventoryDao.deleteInventoryItem(existing)
                                    }
                                }
                            }
                        }

                        // HUD MAJ
                        _consequenceState.value = turnData.consequence_immediate
                        turnData.meteo?.let { meteo ->
                            if (meteo.isNotBlank()) _currentWeather.value = meteo
                        }
                        turnData.resume_action?.let { resume ->
                            if (resume.isNotBlank()) _actionSummary.value = resume
                        }

                        // Agenda MAJ
                        turnData.nouveaux_evenements_agenda?.forEach { event ->
                            val safeTitre = event.titre.trim()
                            val safeDescription = event.description.trim()
                            val safeEcheance = event.echeance.trim()
                            if (safeTitre.isNotEmpty()) {
                                agendaDao.insertAgendaItem(
                                    com.example.resiliencesandbox.data.local.entity.AgendaEntity(
                                        id = 0,
                                        title = safeTitre,
                                        description = safeDescription,
                                        echeance = safeEcheance
                                    )
                                )
                            }
                        }
                        
                        // Affections MAJ
                        turnData.nouvelles_affections?.forEach { affection ->
                            val safeName = affection.nom.trim()
                            if (safeName.isNotEmpty()) {
                                affectionDao.insertAffection(
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
                                affectionDao.deleteAffectionByName(safeName)
                            }
                        }

                        // Compétences MAJ
                        val currentMinutes = repository.getCharacterState()?.gameTimeMinutes ?: 0L
                        turnData.competences_ameliorees?.forEach { (nomCompetence, gain) ->
                            val safeName = nomCompetence.trim()
                            if (safeName.isNotEmpty()) {
                                val existingSkill = skillDao.getSkillByName(safeName)
                                if (existingSkill != null) {
                                    val newLevel = (existingSkill.level + gain).coerceAtMost(100)
                                    skillDao.insertSkill(existingSkill.copy(level = newLevel, lastUsedMinute = currentMinutes))
                                } else {
                                    // Création de la compétence sans plancher (commence directement au niveau du gain)
                                    val newLevel = gain.coerceAtMost(100)
                                    skillDao.insertSkill(com.example.resiliencesandbox.data.local.entity.SkillEntity(safeName, newLevel, currentMinutes))
                                }
                            }
                        }
                        
                        // Rafraîchissement des StateFlows pour l'UI
                        refreshWorldData()
                    } catch (e: Exception) {
                        Log.e("GameViewModel", "JSON Parsing Error: ${e.message}\nJSON: $cleanJson")
                        val actualError = "\n\nErreur JSON : ${e.message}\nJSON généré : $cleanJson"
                        _narrativeText.value = storyLog + actualError
                        storyLog += actualError
                    }
                } else {
                    Log.e("GameViewModel", "Missing <DATA> tag")
                    val actualError = "\n\nErreur IA : Balise <DATA> manquante."
                    _narrativeText.value = storyLog + actualError
                    storyLog += actualError
                }

            } catch (e: Exception) {
                Log.e("GameViewModel", "Crash du modèle LiteRT", e)
                val actualError = "\n\nErreur modèle : ${e.message}"
                _narrativeText.value = storyLog + actualError
                storyLog += actualError
            } finally {
                _isThinking.value = false
                if (taskEndTime == null) {
                    resetIdleTimer()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // On pourrait libérer les ressources du modèle ici si l'orchestrateur est détruit
        // liteRtManager.close()
    }

    private suspend fun updateCharacterStats(peur: Int?, fatigue: Int?, toxicite: Int?, energie: Int?, lieu: String?): TaskDeltas {
        val currentState = repository.getCharacterState() ?: return TaskDeltas(0,0,0,0)
        
        // 1. Mise à jour temporelle : +1 min par action
        val newGameTime = currentState.gameTimeMinutes + 1L
        
        // 2. Application des modificateurs de jauges avec limites (0-100)
        var newPeur = peur?.let { (currentState.peur + it).coerceIn(0, 100) } ?: currentState.peur
        var newFatigue = fatigue?.let { (currentState.fatigue + it).coerceIn(0, 100) } ?: currentState.fatigue
        var newEnergie = energie?.let { (currentState.energie + it).coerceIn(0, 100) } ?: currentState.energie
        var newToxicite = toxicite?.let { (currentState.toxicite + it).coerceIn(0, 100) } ?: currentState.toxicite
        
        // 3. Dégénérescence active via les affections
        var lastDegenerationMinute = currentState.lastDegenerationMinute
        val hoursElapsed = (newGameTime - lastDegenerationMinute) / 60
        if (hoursElapsed >= 1) {
            val activeAffections = affectionDao.getAllAffections()
            val malusCount = activeAffections.size * hoursElapsed.toInt()
            if (malusCount > 0) {
                newEnergie = (newEnergie - (3 * malusCount)).coerceIn(0, 100)
                newFatigue = (newFatigue + (2 * malusCount)).coerceIn(0, 100)
            }
            lastDegenerationMinute += hoursElapsed * 60
        }

        // 4. Dégénérescence des compétences (-1 pt toutes les 96h, plancher à 10)
        val allSkills = skillDao.getAllSkills()
        allSkills.forEach { skill ->
            val hoursSinceUsed = (newGameTime - skill.lastUsedMinute) / 60
            if (hoursSinceUsed >= 96) {
                val dropInstances = (hoursSinceUsed / 96).toInt()
                val newLevel = (skill.level - dropInstances).coerceAtLeast(10)
                if (newLevel != skill.level) {
                    val newLastUsedMinute = skill.lastUsedMinute + (dropInstances * 96 * 60)
                    skillDao.insertSkill(skill.copy(level = newLevel, lastUsedMinute = newLastUsedMinute))
                }
            }
        }

        val updatedState = currentState.copy(
            peur = newPeur,
            fatigue = newFatigue,
            energie = newEnergie,
            toxicite = newToxicite,
            gameTimeMinutes = newGameTime,
            lastDegenerationMinute = lastDegenerationMinute
        )
        repository.updateCharacter(updatedState)
        return TaskDeltas(newPeur - currentState.peur, newFatigue - currentState.fatigue, newEnergie - currentState.energie, newToxicite - currentState.toxicite)
    }

    private fun sendSystemNotification(title: String, message: String) {
        val channelId = "task_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tâches en jeu",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications liées à l'achèvement des tâches"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            Log.e("GameViewModel", "Missing POST_NOTIFICATIONS permission", e)
        }
    }

    fun getSemanticRelation(score: Int): String {
        return when (score) {
            in 0..20 -> "Hostilité ouverte"
            in 21..40 -> "Méfiance"
            in 41..60 -> "Neutre / Inconnu"
            in 61..80 -> "Confiance naissante"
            else -> "Allié fidèle"
        }
    }
}
