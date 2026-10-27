package cl.belmontespa.myapplication.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SesionStoreTest {

    @Test
    fun sinDatosAbreSinSesion() = runTest {
        val store = SesionStore(
            PreferenciasEnMemoria(),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
        )

        assertEquals(EstadoSesion.SinSesion, store.estado.first())
    }

    @Test
    fun restauraLaSesionGuardada() = runTest {
        val preferencias = PreferenciasEnMemoria()
        val reloj = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        val sesion = sesion(expiraEn = 60_000)

        SesionStore(preferencias, reloj).guardar(sesion)

        // Otra instancia representa una nueva apertura de la aplicación.
        val restaurada = SesionStore(preferencias, reloj)
        assertEquals(EstadoSesion.Autenticada(sesion), restaurada.estado.first())
    }

    @Test
    fun sesionTemporalNoRecuperaLaCuentaAnteriorAlReabrir() = runTest {
        val preferencias = PreferenciasEnMemoria()
        val reloj = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        val store = SesionStore(preferencias, reloj)
        val anterior = sesion(expiraEn = 60_000)
        val temporal = anterior.copy(
            token = "token-temporal",
            correo = "otra@ejemplo.cl",
        )

        store.guardar(anterior)
        store.guardarTemporal(temporal)

        assertEquals(EstadoSesion.Temporal(temporal), store.estado.first())

        // La instancia nueva no tiene acceso a la sesión que vivía en memoria.
        val reabierta = SesionStore(preferencias, reloj)
        assertEquals(EstadoSesion.SinSesion, reabierta.estado.first())
        assertEquals(anterior.correo, reabierta.ultimaCuenta.first())
    }

    @Test
    fun cerrarSesionConservaLaUltimaCuenta() = runTest {
        val store = SesionStore(
            PreferenciasEnMemoria(),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
        )
        val sesion = sesion(expiraEn = 60_000)

        store.guardar(sesion)
        store.borrar()

        assertEquals(EstadoSesion.SinSesion, store.estado.first())
        assertEquals(sesion.correo, store.ultimaCuenta.first())
    }

    @Test
    fun ambasClasesDeSesionVencenSinModificarPreferencias() = runTest {
        val reloj = RelojDePrueba(ahora = { testScheduler.currentTime })
        val normal = SesionStore(PreferenciasEnMemoria(), reloj)
        val temporal = SesionStore(PreferenciasEnMemoria(), reloj)
        val sesion = sesion(expiraEn = 1_000)

        normal.guardar(sesion)
        temporal.guardarTemporal(sesion)

        val estadosNormales = mutableListOf<EstadoSesion>()
        val estadosTemporales = mutableListOf<EstadoSesion>()

        // Las suscripciones se cancelan automáticamente al terminar la prueba.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            normal.estado.collect { estadosNormales.add(it) }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            temporal.estado.collect { estadosTemporales.add(it) }
        }
        runCurrent()

        assertEquals(EstadoSesion.Autenticada(sesion), estadosNormales.last())
        assertEquals(EstadoSesion.Temporal(sesion), estadosTemporales.last())

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(EstadoSesion.SinSesion, estadosNormales.last())
        assertEquals(EstadoSesion.SinSesion, estadosTemporales.last())
    }

    private fun sesion(expiraEn: Long) = Sesion(
        token = "token-prueba",
        correo = "cuenta@ejemplo.cl",
        rol = Rol.SOLICITANTE,
        expiraEn = expiraEn,
    )

    // Sustituye solo el almacenamiento en estas pruebas unitarias; la aplicación usa el DataStore real.
    private class PreferenciasEnMemoria : DataStore<Preferences> {
        private val contenido = MutableStateFlow(emptyPreferences())
        private val escrituras = Mutex()
        override val data: Flow<Preferences> = contenido

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences,
        ): Preferences = escrituras.withLock {
            transform(contenido.value).also { contenido.value = it }
        }
    }

    // Clock y las esperas de las corrutinas comparten el mismo tiempo virtual.
    private class RelojDePrueba(
        private val ahora: () -> Long,
        private val zona: ZoneId = ZoneOffset.UTC,
    ) : Clock() {
        override fun getZone(): ZoneId = zona
        override fun withZone(zone: ZoneId): Clock = RelojDePrueba(ahora, zone)
        override fun instant(): Instant = Instant.ofEpochMilli(ahora())
    }
}
