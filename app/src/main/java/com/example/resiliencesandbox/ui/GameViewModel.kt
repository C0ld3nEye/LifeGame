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
                val response = liteRtManager.generateResponse(prompt)

                // Injection du résultat dans l'UI
                _narrativeText.value = response

            } catch (e: Exception) {
                Log.e("GameViewModel", "Crash du modèle LiteRT", e)
                // Message narratif de secours en cas d'erreur
                _narrativeText.value = "Mes pensées sont trop confuses pour analyser ça..."
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
}
