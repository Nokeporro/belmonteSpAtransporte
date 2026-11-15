package cl.belmontespa.myapplication.notificaciones

import android.app.Application
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.repository.AuthRepository
import cl.belmontespa.myapplication.repository.SoporteLocal
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class TokenFcmWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        if (!notificacionesRemotasHabilitadas()) return Result.success()
        val auth = AuthRepository(applicationContext as Application)
        return when (registrarTokenFcm(auth.observarSesion().first(),
            PreferenciasNotificaciones(applicationContext).leerToken(), auth::actualizarTokenFcm)) {
            ResultadoRegistroToken.LISTO -> Result.success()
            ResultadoRegistroToken.REINTENTAR -> Result.retry()
        }
    }

    companion object {
        const val TRABAJO_UNICO = "registro_token_fcm"

        fun programar(contexto: Context) {
            if (!notificacionesRemotasHabilitadas()) return
            val pedido = OneTimeWorkRequestBuilder<TokenFcmWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(contexto).enqueueUniqueWork(TRABAJO_UNICO, ExistingWorkPolicy.REPLACE, pedido)
        }
    }
}

internal fun notificacionesRemotasHabilitadas(): Boolean =
    BuildConfig.FIREBASE_CONFIGURADO && !SoporteLocal.modoLocalPorDefecto
