package cl.belmontespa.myapplication.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

interface ProgramadorSincronizacion {
    fun programar()

    object Ninguno : ProgramadorSincronizacion {
        override fun programar() = Unit
    }
}

class ProgramadorWorkManager(private val contexto: Context) : ProgramadorSincronizacion {
    override fun programar() {
        val pedido = OneTimeWorkRequestBuilder<SincronizacionWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto).enqueueUniqueWork(TRABAJO_UNICO, ExistingWorkPolicy.APPEND_OR_REPLACE, pedido)
    }

    companion object {
        const val TRABAJO_UNICO = "sincronizacion"
    }
}
