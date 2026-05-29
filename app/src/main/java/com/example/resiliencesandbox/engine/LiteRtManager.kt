package com.example.resiliencesandbox.engine

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LiteRtManager(private val context: Context) {

    private var llmInference: LlmInference? = null
    
    companion object {
        private const val TAG = "LiteRtManager"
    }

    /**
     * Initialise le modèle LLM en local via le SDK LiteRT.
     * Configuration stricte pour éviter les crashs RAM et le fallback CPU.
     */
    suspend fun initializeModel(modelPath: String) = withContext(Dispatchers.IO) {
        if (llmInference != null) {
            Log.d(TAG, "Le modèle est déjà initialisé.")
            return@withContext
        }

        val file = File(modelPath)
        if (!file.exists()) {
            throw IllegalArgumentException("Fichier modèle introuvable à l'emplacement : $modelPath")
        }

        Log.d(TAG, "Initialisation de LiteRT-LM (Gemma) avec accélération matérielle...")

        // Configuration explicite : Bridage RAM strict (200 tokens max en sortie)
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(200) // Bridage strict pour préserver la RAM et la batterie
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
        Log.d(TAG, "Modèle initialisé avec succès.")
    }

    /**
     * Génère une réponse textuelle depuis l'IA en local.
     */
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val inference = llmInference ?: throw IllegalStateException("Le modèle LiteRT n'est pas initialisé. Appelez initializeModel() en premier.")
        
        Log.d(TAG, "Génération en cours...")
        // generateResponse est une méthode synchrone de LiteRT-LM, on la tourne dans Dispatchers.IO
        val response = inference.generateResponse(prompt)
        
        Log.d(TAG, "Génération terminée.")
        return@withContext response
    }

    /**
     * Libère les ressources du modèle (très important pour la RAM).
     */
    fun close() {
        llmInference?.close()
        llmInference = null
        Log.d(TAG, "Ressources du modèle libérées.")
    }
}
