package cl.belmontespa.myapplication.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.belmontespa.myapplication.database.AppDatabase
import cl.belmontespa.myapplication.repository.SoporteLocal
import cl.belmontespa.myapplication.session.SesionStore
import cl.belmontespa.myapplication.util.Reloj

class SincronizacionWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        val local = SoporteLocal(AppDatabase.getDatabase(applicationContext), SesionStore.getInstance(applicationContext), Reloj.santiago)
        return when (SincronizadorDeCola(local).enviar()) {
            ResultadoSync.REINTENTAR -> Result.retry()
            ResultadoSync.LISTO, ResultadoSync.ESPERAR_SESION -> Result.success()
        }
    }
}
