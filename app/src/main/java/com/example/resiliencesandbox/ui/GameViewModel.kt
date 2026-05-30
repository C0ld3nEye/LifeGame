package com.example.resiliencesandbox.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.resiliencesandbox.data.CharacterRepository
import com.example.resiliencesandbox.data.local.entity.CharacterEntity
import com.example.resiliencesandbox.domain.ContextInjector
import com.example.resiliencesandbox.engine.LiteRtManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@Serializable
data class TurnData(
    val peur: Int? = null,
    val fatigue: Int? = null,
    val lieu: String? = null
)

class GameViewModel(
    private val repository: CharacterRepository,
    private val contextInjector: ContextInjector,
    private val liteRtManager: LiteRtManager
) : ViewModel() {

    // 1. Observation en continu de l'état du personnage (Base de Données -> UI)
    val characterState: StateFlow<CharacterEntity?> = repository.characterFlow
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

    /**
     * Traite l'action entrée par le joueur, construit le prompt omniscient,
     * l'envoie au modèle et met à jour la réponse narrativement.
     */
    fun submitPlayerAction(userText: String) {
        viewModelScope.launch {
            _isThinking.value = true

            try {
                // Récupération de la donnée + formatage strict (Pont de données)
                val prompt = contextInjector.buildOmniscientPrompt(userText)

                // Exécution asynchrone du LLM en local
                val rawResponse = liteRtManager.generateResponse(prompt)

                // Injection du résultat dans l'UI
                // Supprime tout ce qui se trouve à partir de la balise <DATA>
                val textePropre = rawResponse.substringBefore("<DATA>").trim()
                _narrativeText.value = textePropre

                // On isole le JSON, en gérant le cas où le LLM oublie la balise de fin
                val jsonString = rawResponse.substringAfter("<DATA>", missingDelimiterValue = "")
                    .substringBefore("</DATA>")
                    .trim()

                if (jsonString.isNotEmpty()) {
                    try {
                        // Un parseur permissif pour encaisser les erreurs de formatage du petit LLM
                        val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
                        val turnData = jsonParser.decodeFromString<TurnData>(jsonString)

                        // Mise à jour de l'état du personnage via ton repository ou tes MutableStateFlow
                        updateCharacterStats(
                            peur = turnData.peur,
                            fatigue = turnData.fatigue,
                            lieu = turnData.lieu
                        )
                    } catch (e: Exception) {
                        // Le LLM a halluciné un JSON pété, on logue et on passe au tour suivant sans faire crasher l'app
                        Log.e("GameViewModel", "JSON parsing failed on turn: ${e.message} \nRaw JSON was: $jsonString")
                    }
                }

            } catch (e: Exception) {
                Log.e("GameViewModel", "Crash du modèle LiteRT", e)
                // Message narratif de secours en cas d'erreur
                _narrativeText.value = "Erreur IA : ${e.message}"
            } finally {
                _isThinking.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // On pourrait libérer les ressources du modèle ici si l'orchestrateur est détruit
        // liteRtManager.close()
    }

    private fun updateCharacterStats(peur: Int?, fatigue: Int?, lieu: String?) {
        viewModelScope.launch {
            val currentState = repository.getCharacterState() ?: return@launch
            
            // Application des deltas générés par l'IA en s'assurant de rester entre 0 et 100
            val newPeur = peur?.let { (currentState.peur + it).coerceIn(0, 100) } ?: currentState.peur
            val newFatigue = fatigue?.let { (currentState.fatigue + it).coerceIn(0, 100) } ?: currentState.fatigue
            
            val updatedState = currentState.copy(
                peur = newPeur,
                fatigue = newFatigue
            )
            repository.updateCharacter(updatedState)
        }
    }
}
