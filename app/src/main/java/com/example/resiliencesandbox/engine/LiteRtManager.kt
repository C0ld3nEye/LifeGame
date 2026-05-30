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

        val engineConfig = EngineConfig(
            modelPath = modelPath
        )

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
            // sendMessage renvoie un objet Message dont le contenu doit être extrait
            val message = conversation.sendMessage(prompt)
            responseText = message.contents.contents.filterIsInstance<com.google.ai.edge.litertlm.Content.Text>().joinToString("") { it.text }
        }
        
        Log.d(TAG, "Génération terminée.")
        return@withContext responseText
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
