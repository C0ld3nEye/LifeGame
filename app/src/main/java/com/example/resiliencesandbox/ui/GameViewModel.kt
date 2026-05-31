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

data class NotificationData(val id: Long, val text: String)

@Serializable
data class ItemData(
    val nom: String,
    val etat: String,
    val description: String
)

@Serializable
data class TurnData(
    val peur: Int? = null,
    val fatigue: Int? = null,
    val energie: Int? = null,
    val toxicite: Int? = null,
    val lieu: String? = null,
    val nouveaux_pnj: List<String>? = null,
    val nouveaux_objets: List<ItemData>? = null
)

data class SemanticCharacterState(
    val energie: String,
    val fatigue: String,
    val peur: String,
    val toxicite: String
)

class GameViewModel(
    private val repository: CharacterRepository,
    private val contextInjector: ContextInjector,
    private val liteRtManager: LiteRtManager,
    private val locationDao: LocationDao,
    private val npcDao: NpcDao,
    private val inventoryDao: InventoryDao
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

    // 3. StateFlow pour indiquer si l'IA réfléchit (temps de chargement)
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    // 4. Mémoire à court terme pour éviter l'amnésie conversationnelle immédiate
    private var lastAiResponse: String = "L'obscurité t'entoure. Que fais-tu ?"
    private var storyLog: String = "L'obscurité t'entoure. Que fais-tu ?"

    // Notifications flottantes
    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications.asStateFlow()

    private fun pushNotification(text: String) {
        viewModelScope.launch {
            val newNotif = NotificationData(System.currentTimeMillis(), text)
            _notifications.value = _notifications.value + newNotif
            kotlinx.coroutines.delay(3000)
            _notifications.value = _notifications.value.filter { it.id != newNotif.id }
        }
    }

    // 5. Exposition des données du monde (Panneau de Conscience)
    private val _inventoryState = MutableStateFlow<List<InventoryEntity>>(emptyList())
    val inventoryState: StateFlow<List<InventoryEntity>> = _inventoryState.asStateFlow()

    private val _npcsState = MutableStateFlow<List<NpcEntity>>(emptyList())
    val npcsState: StateFlow<List<NpcEntity>> = _npcsState.asStateFlow()

    private val _currentLocationName = MutableStateFlow("Inconnu")
    val currentLocationName: StateFlow<String> = _currentLocationName.asStateFlow()

    private val _locationsState = MutableStateFlow<List<LocationEntity>>(emptyList())
    val locationsState: StateFlow<List<LocationEntity>> = _locationsState.asStateFlow()

    private var idleTimerJob: Job? = null

    private fun stopIdleTimer() {
        idleTimerJob?.cancel()
    }

    private fun resetIdleTimer() {
        idleTimerJob?.cancel()
        idleTimerJob = viewModelScope.launch {
            delay(60_000L) // 60 secondes d'inactivité
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
            }
            refreshWorldData()
            triggerWakeUpIfNeeded()
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
            _currentLocationName.value = locations.lastOrNull()?.name ?: "Inconnu"
        }
    }

    /**
     * Traite l'action entrée par le joueur, construit le prompt omniscient,
     * l'envoie au modèle et met à jour la réponse narrativement.
     */
    fun submitPlayerAction(userText: String, isWakeUp: Boolean = false, isHidden: Boolean = false) {
        stopIdleTimer()
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
                val prompt = contextInjector.buildOmniscientPrompt(userText, lastAiResponse, crisisReason, isWakeUp)

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
                    val cleanJson = matchResult.groupValues[1].trim().replace("'", "\"")
                    
                    try {
                        val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
                        val turnData = jsonParser.decodeFromString<TurnData>(cleanJson)

                        // Mise à jour de l'état du personnage via ton repository ou tes MutableStateFlow
                        updateCharacterStats(
                            peur = turnData.peur,
                            fatigue = turnData.fatigue,
                            toxicite = turnData.toxicite,
                            energie = turnData.energie,
                            lieu = turnData.lieu
                        )

                        // 1. Enregistrement du Lieu
                        turnData.lieu?.let { locName ->
                            if (locName.isNotBlank() && locName.lowercase() != "inconnu") {
                                // On vérifie s'il existe déjà
                                val existing = locationDao.getLocationById(locName)
                                if (existing == null) {
                                    locationDao.insertLocation(
                                        LocationEntity(
                                            id = locName,
                                            name = locName,
                                            description = "Découvert récemment.",
                                            isDiscovered = true,
                                            visitCount = 1
                                        )
                                    )
                                } else {
                                    locationDao.insertLocation(
                                        existing.copy(visitCount = existing.visitCount + 1)
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
                        
                        // Rafraîchissement des StateFlows pour l'UI
                        refreshWorldData()
                    } catch (e: Exception) {
                        // INJECTION VISUELLE OBLIGATOIRE DE L'ERREUR
                        val errorMsg = "\n\n[ERREUR CRITIQUE PARSING JSON]\nException: ${e.message}\nJSON Tenté: $cleanJson"
                        _narrativeText.value += errorMsg
                        storyLog += errorMsg
                    }
                } else {
                    // INJECTION VISUELLE OBLIGATOIRE SI BALISE ABSENTE
                    val errorMsg = "\n\n[ERREUR CRITIQUE : La balise <DATA> est introuvable ou mal formatée par l'IA.]"
                    _narrativeText.value += errorMsg
                    storyLog += errorMsg
                }

            } catch (e: Exception) {
                Log.e("GameViewModel", "Crash du modèle LiteRT", e)
                // Message narratif de secours en cas d'erreur
                _narrativeText.value = "Erreur IA : ${e.message}"
            } finally {
                _isThinking.value = false
                resetIdleTimer()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // On pourrait libérer les ressources du modèle ici si l'orchestrateur est détruit
        // liteRtManager.close()
    }

    private fun updateCharacterStats(peur: Int?, fatigue: Int?, toxicite: Int?, energie: Int?, lieu: String?) {
        viewModelScope.launch {
            val currentState = repository.getCharacterState() ?: return@launch
            
            // Avancement du temps (entre 15 et 60 minutes)
            val elapsedMinutes = (15..60).random().toLong()
            val newTime = currentState.gameTimeMinutes + elapsedMinutes
            
            // Malus passif
            val baseFatigue = (currentState.fatigue + 2).coerceIn(0, 100)
            
            // Application des deltas générés par l'IA en s'assurant de rester entre 0 et 100
            val newPeur = peur?.let { (currentState.peur + it).coerceIn(0, 100) } ?: currentState.peur
            val finalFatigue = fatigue?.let { (baseFatigue + it).coerceIn(0, 100) } ?: baseFatigue
            val diffFatigue = finalFatigue - currentState.fatigue

            val newToxicite = toxicite?.let { (currentState.toxicite + it).coerceIn(0, 100) } ?: currentState.toxicite
            
            val energieMax = 100 - (newToxicite / 2)
            val newEnergie = (currentState.energie - 1).coerceIn(0, energieMax)
            val finalEnergie = energie?.let { (newEnergie + it).coerceIn(0, energieMax) } ?: newEnergie
            
            val diffEnergie = finalEnergie - currentState.energie
            val diffPeur = newPeur - currentState.peur
            
            if (diffFatigue > 0) pushNotification("Vos paupières sont lourdes")
            else if (diffFatigue < 0) pushNotification("Vous vous sentez plus reposé")
            
            if (diffEnergie < 0) pushNotification("Votre énergie s'épuise")
            else if (diffEnergie > 0) pushNotification("Regain d'énergie")

            if (diffPeur > 0) pushNotification("L'angoisse monte")
            else if (diffPeur < 0) pushNotification("Vous vous calmez un peu")
            
            val updatedState = currentState.copy(
                peur = newPeur,
                fatigue = finalFatigue,
                energie = finalEnergie,
                toxicite = newToxicite,
                gameTimeMinutes = newTime
            )
            repository.updateCharacter(updatedState)
        }
    }

    fun getSemanticRelation(score: Int): String {
        return when {
            score <= -50 -> "Hostile"
            score in -49..-10 -> "Méfiant"
            score in -9..9 -> "Inconnu / Neutre"
            score in 10..49 -> "Contact régulier"
            score in 50..89 -> "Amical"
            else -> "Allié fidèle"
        }
    }
}
