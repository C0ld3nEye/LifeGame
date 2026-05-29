package com.example.resiliencesandbox

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.resiliencesandbox.data.CharacterRepository
import com.example.resiliencesandbox.data.local.AppDatabase
import com.example.resiliencesandbox.domain.ContextInjector
import com.example.resiliencesandbox.engine.LiteRtManager
import com.example.resiliencesandbox.theme.ResilienceSandboxTheme
import com.example.resiliencesandbox.ui.GameViewModel
import com.example.resiliencesandbox.ui.screens.GameScreen
import com.example.resiliencesandbox.worker.RoutineScheduler
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var liteRtManager: LiteRtManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Gestion des permissions pour Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        // Lancement de l'orchestrateur temporel mathématique (étape 4)
        RoutineScheduler.startRoutine(this)

        // Instanciation de l'architecture Backend (Injection manuelle)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CharacterRepository(database.characterDao(), database.eventLogDao())
        val contextInjector = ContextInjector(repository)
        liteRtManager = LiteRtManager(applicationContext)

        // Factory pour injecter les dépendances dans le GameViewModel
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return GameViewModel(repository, contextInjector, liteRtManager) as T
                }
                throw IllegalArgumentException("Classe ViewModel inconnue")
            }
        }

        val gameViewModel = ViewModelProvider(this, viewModelFactory)[GameViewModel::class.java]

        // Initialisation non bloquante de l'IA (en attente d'un modèle copié)
        lifecycleScope.launch {
            try {
                // Remplacer "gemma.litertlm" par le nom final ou utiliser un sélecteur de fichier
                val modelFile = File(filesDir, "gemma.litertlm")
                if (modelFile.exists()) {
                    liteRtManager.initializeModel(modelFile.absolutePath)
                } else {
                    Log.w("MainActivity", "Le modèle LiteRT n'est pas encore présent dans les fichiers. (${modelFile.absolutePath})")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur d'initialisation de LiteRT", e)
            }
        }

        enableEdgeToEdge()
        setContent {
            ResilienceSandboxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Affichage de l'interface brutaliste et du shader Rorschach
                    GameScreen(viewModel = gameViewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::liteRtManager.isInitialized) {
            liteRtManager.close()
        }
    }
}
