package cl.belmontespa.myapplication.viewmodel.abordaje

import cl.belmontespa.myapplication.model.CodigoAbordaje
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.TipoDocumento
import cl.belmontespa.myapplication.repository.AbordajeRepository
import cl.belmontespa.myapplication.viewmodel.aplicacionFalsa
import cl.belmontespa.myapplication.viewmodel.soporteVacio
import java.time.LocalDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** «Confirmar abordaje» (HU29): documento válido, envío único, errores que conservan el formulario y documento que no queda. */
@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmarAbordajeViewModelTest {

    private val repositorio = RepositorioAbordajeFalso()
    private val marca = LocalDateTime.of(2026, 10, 28, 14, 5, 9)

    @Before
    fun prepararHilo() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun soltarHilo() = Dispatchers.resetMain()

    private fun crear(argumento: String = "KXJR42:7F3A9C21B04E58D6") = ConfirmarAbordajeViewModel(aplicacionFalsa(), repositorio).also { it.cargar(argumento) }

    @Test
    fun cargaElVehiculoDeLaRutaConRutComoTipoInicial() {
        val estado = crear().estado.value

        assertEquals(CodigoAbordaje("KXJR42", "7F3A9C21B04E58D6"), estado.codigo)
        assertEquals(TipoDocumento.RUT, estado.tipoDocumento)
        assertEquals("", estado.documento)
        assertFalse(estado.puedeConfirmar)
    }

    @Test
    fun unArgumentoDeRutaInvalidoDejaElVehiculoVacioYNoSePuedeConfirmar() {
        val vm = crear("cualquier cosa")
        vm.escribirDocumento("15.234.567-4")

        assertNull(vm.estado.value.codigo)
        assertFalse(vm.estado.value.puedeConfirmar)
        vm.confirmar()
        assertTrue(repositorio.llamadas.isEmpty())
    }

    @Test
    fun cargarElMismoArgumentoDeNuevoConservaLoEscrito() {
        val vm = crear()
        vm.escribirDocumento("15.234.567-4")

        vm.cargar("KXJR42:7F3A9C21B04E58D6")

        assertEquals("15.234.567-4", vm.estado.value.documento)
    }

    @Test
    fun cargarOtroVehiculoEmpiezaDeCero() {
        val vm = crear()
        vm.escribirDocumento("15.234.567-4")

        vm.cargar("GZRW90:9D04C6E2A1F7B853")

        assertEquals("", vm.estado.value.documento)
        assertEquals("GZRW90", vm.estado.value.codigo?.patente)
    }

    @Test
    fun unDocumentoInvalidoSeMarcaSinLlamarAlRepositorio() {
        val vm = crear()
        vm.escribirDocumento("15.234.567-0")

        vm.confirmar()

        assertTrue(vm.estado.value.documentoInvalido)
        assertFalse(vm.estado.value.enviando)
        assertTrue(repositorio.llamadas.isEmpty())
    }

    @Test
    fun corregirElDocumentoQuitaLaMarcaDeInvalido() {
        val vm = crear()
        vm.escribirDocumento("15.234.567-0")
        vm.confirmar()

        vm.escribirDocumento("15.234.567-4")

        assertFalse(vm.estado.value.documentoInvalido)
    }

    @Test
    fun unPasaporteValidoSeEnviaConSuTipo() {
        val vm = crear()
        vm.elegirTipo(TipoDocumento.PASAPORTE)
        vm.escribirDocumento("P4471023")
        repositorio.respuesta = Resultado.Exito(marca)

        vm.confirmar()

        assertEquals(listOf(Triple(CodigoAbordaje("KXJR42", "7F3A9C21B04E58D6"), TipoDocumento.PASAPORTE, "P4471023")), repositorio.llamadas)
    }

    @Test
    fun elExitoGuardaLaHoraYBorraElDocumentoDelEstado() {
        val vm = crear()
        vm.escribirDocumento("15.234.567-4")
        repositorio.respuesta = Resultado.Exito(marca)

        vm.confirmar()

        val estado = vm.estado.value
        assertEquals(marca, estado.registradoA)
        assertEquals("", estado.documento)
        assertFalse(estado.enviando)
        assertNull(estado.error)
        assertFalse(estado.puedeConfirmar)
    }

    @Test
    fun elNoEncontradoConservaElFormularioParaReintentar() {
        val vm = crear()
        vm.escribirDocumento("15.234.567-4")
        repositorio.respuesta = Resultado.Fallo(ErrorApp.NoEncontrado)

        vm.confirmar()

        val estado = vm.estado.value
        assertEquals(ErrorApp.NoEncontrado, estado.error)
        assertEquals("15.234.567-4", estado.documento)
        assertNull(estado.registradoA)
        assertFalse(estado.enviando)
        assertTrue(estado.puedeConfirmar)
    }

    @Test
    fun sinConexionConservaElFormularioYSePuedeReintentar() {
        val vm = crear()
        vm.escribirDocumento("15.234.567-4")
        repositorio.respuesta = Resultado.Fallo(ErrorApp.SinConexion)
        vm.confirmar()
        assertEquals(ErrorApp.SinConexion, vm.estado.value.error)

        repositorio.respuesta = Resultado.Exito(marca)
        vm.confirmar()

        assertEquals(marca, vm.estado.value.registradoA)
        assertNull(vm.estado.value.error)
        assertEquals(2, repositorio.llamadas.size)
    }

    @Test
    fun escribirDeNuevoQuitaElErrorDelServidor() {
        val vm = crear()
        vm.escribirDocumento("15.234.567-4")
        repositorio.respuesta = Resultado.Fallo(ErrorApp.NoEncontrado)
        vm.confirmar()

        vm.elegirTipo(TipoDocumento.PASAPORTE)

        assertNull(vm.estado.value.error)
    }

    @Test
    fun unSegundoToqueMientrasSeEnviaNoLlamaOtraVez() {
        val vm = crear()
        vm.escribirDocumento("15.234.567-4")
        val pendiente = CompletableDeferred<Resultado<LocalDateTime>>()
        repositorio.pendiente = pendiente

        vm.confirmar()
        assertTrue(vm.estado.value.enviando)
        vm.confirmar()

        assertEquals(1, repositorio.llamadas.size)
        pendiente.complete(Resultado.Exito(marca))
        assertEquals(marca, vm.estado.value.registradoA)
        assertEquals(1, repositorio.llamadas.size)
    }

    @Test
    fun elEstadoNoMuestraElDocumentoEnSuTexto() {
        val vm = crear()
        vm.escribirDocumento("15.234.567-4")

        val texto = vm.estado.value.toString()

        assertFalse(texto, texto.contains("15.234.567"))
        assertTrue(texto, texto.contains("KXJR42"))
    }

    @Test
    fun trasElExitoNoSePuedeVolverAConfirmar() {
        val vm = crear()
        vm.escribirDocumento("15.234.567-4")
        repositorio.respuesta = Resultado.Exito(marca)
        vm.confirmar()
        vm.escribirDocumento("15.234.567-4")

        vm.confirmar()

        assertEquals(1, repositorio.llamadas.size)
    }
}

private class RepositorioAbordajeFalso : AbordajeRepository(soporteVacio()) {
    val llamadas = mutableListOf<Triple<CodigoAbordaje, TipoDocumento, String>>()
    var respuesta: Resultado<LocalDateTime> = Resultado.Fallo(ErrorApp.NoEncontrado)

    /** Si se pone, la llamada espera a que la prueba la complete: simula una respuesta lenta. */
    var pendiente: CompletableDeferred<Resultado<LocalDateTime>>? = null

    override suspend fun registrarAbordaje(codigo: CodigoAbordaje, tipoDocumento: TipoDocumento, dni: String): Resultado<LocalDateTime> {
        llamadas += Triple(codigo, tipoDocumento, dni)
        return pendiente?.await() ?: respuesta
    }
}
