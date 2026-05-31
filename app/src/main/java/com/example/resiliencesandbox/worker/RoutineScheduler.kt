package com.example.resiliencesandbox.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RoutineScheduler {
    private const val ROUTINE_WORK_NAME = "PeriodicRoutineTickWorker"

    /**
     * Lance le Worker de routine périodiquement (Toutes les 2 heures).
     */
    fun startRoutine(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<RoutineTickWorker>(
            2, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ROUTINE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Garder la tâche existante si elle tourne déjà
            workRequest
        )
    }

    /**
     * Stoppe la routine de fond.
     */
    fun stopRoutine(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ROUTINE_WORK_NAME)
    }
}
