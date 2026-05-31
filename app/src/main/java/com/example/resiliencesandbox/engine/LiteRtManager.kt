package com.example.resiliencesandbox.engine

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LiteRtManager(private val context: Context) {

    private var engine: Engine? = null
    
    companion object {
        private const val TAG = "LiteRtManager"
    }

    /**
     * Charge le modèle LiteRT-LM (format .litertlm).
     */
    suspend fun initializeModel(modelPath: String) = withContext(Dispatchers.IO) {
        if (engine != null) {
            Log.d(TAG, "Le modèle LiteRT-LM est déjà initialisé.")
            return@withContext
        }

        val file = File(modelPath)
        if (!file.exists()) {
            throw IllegalArgumentException("Fichier modèle introuvable à l'emplacement : $modelPath")
        }

        Log.d(TAG, "Initialisation de LiteRT-LM via Engine...")

        // TODO: Configuration GPU à ajouter via EngineConfig si supporté par l'API

        val engineConfig = EngineConfig(
            modelPath = modelPath
        )

        // Nous injectons les options matérielles si l'API le permet.
        // Puisque nous sommes sur l'API Engine, si `EngineConfig` ne prend pas d'options,
        // nous supposons que le backend LiteRT le gérera en interne ou selon la configuration
        // de Model.Options disponible.
        
        val newEngine = Engine(engineConfig)
        newEngine.initialize()
        engine = newEngine
        Log.d(TAG, "Modèle LiteRT-LM initialisé avec succès.")
    }

    /**
     * Génère une réponse synchrone via l'Engine, exécutée en IO.
     */
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val currentEngine = engine ?: throw IllegalStateException("Le modèle LiteRT n'est pas initialisé.")
        
        Log.d(TAG, "Génération en cours...")
        var responseText = ""
        currentEngine.createConversation().use { conversation ->
            val message = conversation.sendMessage(prompt)
            responseText = message.contents.contents.filterIsInstance<com.google.ai.edge.litertlm.Content.Text>().joinToString("") { it.text }
        }
        
        Log.d(TAG, "Génération terminée.")
        return@withContext responseText
    }

    /**
     * Génère une réponse en flux (streaming) via l'Engine.
     */
    fun generateResponseStream(prompt: String): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flow {
        val currentEngine = engine ?: throw IllegalStateException("Le modèle LiteRT n'est pas initialisé.")
        Log.d(TAG, "Génération en streaming en cours...")
        
        currentEngine.createConversation().use { conversation ->
            conversation.sendMessageAsync(prompt).collect { messageChunk ->
                val text = messageChunk.contents.contents.filterIsInstance<com.google.ai.edge.litertlm.Content.Text>().joinToString("") { it.text }
                emit(text)
            }
        }
    }

    /**
     * Fermeture du modèle pour libérer la mémoire native.
     */
    fun close() {
        engine?.close()
        engine = null
        Log.d(TAG, "Ressources du modèle libérées.")
    }
}
