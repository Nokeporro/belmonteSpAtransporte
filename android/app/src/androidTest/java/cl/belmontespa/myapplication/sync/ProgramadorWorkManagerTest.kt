package cl.belmontespa.myapplication.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgramadorWorkManagerTest {

    private val contexto: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun iniciar() = WorkManagerTestInitHelper.initializeTestWorkManager(contexto)

    private fun trabajos() = WorkManager.getInstance(contexto).getWorkInfosForUniqueWork(ProgramadorWorkManager.TRABAJO_UNICO).get()

    @Test
    fun programarDejaUnTrabajoUnicoQueExigeRed() {
        ProgramadorWorkManager(contexto).programar()

        val trabajo = trabajos().single()
        assertEquals(WorkInfo.State.ENQUEUED, trabajo.state)
        assertEquals(NetworkType.CONNECTED, trabajo.constraints.requiredNetworkType)
    }

    @Test
    fun unSegundoPedidoSeEncadenaSinPerderElPrimero() {
        val programador = ProgramadorWorkManager(contexto)

        programador.programar()
        programador.programar()

        assertEquals(setOf(WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED), trabajos().map { it.state }.toSet())
    }
}
