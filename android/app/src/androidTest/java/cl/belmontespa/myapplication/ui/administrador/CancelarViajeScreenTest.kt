package cl.belmontespa.myapplication.ui.administrador

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.TipoRecibo
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** «Cancelar viaje» (HU24) con un estado fijo: lo que se ve y lo que se avisa, sin Room ni ViewModel. */
class CancelarViajeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val textos = InstrumentationRegistry.getInstrumentation().targetContext
    private val agenda = LocalDateTime.of(2026, 10, 24, 6, 30)

    private fun viaje(estado: EstadoViaje = EstadoViaje.PENDIENTE, hashPago: String? = "pago-1"): Viaje {
        val lugar = Ubicacion("Av. Providencia 1234", "Providencia", "Metropolitana de Santiago", -33.4261, -70.611)
        return Viaje(
            folio = "TR-1038", estado = estado, correoSolicitante = "solicitante@ejemplo.cl",
            nombreSolicitante = "Luis Pino", telefonoSolicitante = null, razonSocial = "Empresa",
            origen = lugar, destino = lugar, fhCreacion = agenda.minusDays(1), fhAgenda = agenda,
            fhAsignacion = null, fhInicio = null, fhFin = null, numeroVuelo = null, cantidadPasajeros = 1,
            correoConductor = null, nombreConductor = null, patenteMovil = null, hashPago = hashPago,
            estadoPago = null, motivo = null, pasajeros = emptyList(),
        )
    }

    private fun pago(estado: EstadoPago = EstadoPago.PAGADO) = Pago(
        "pago-1", "TR-1038", 52_000, estado, "Banco de prueba", 12, "76543210-3", MedioPago.TRANSFERENCIA,
        TipoRecibo.FACTURA, agenda.minusDays(2), null,
    )

    private fun estado(
        viaje: Viaje? = viaje(),
        pago: Pago? = pago(),
        vencido: Boolean = true,
    ) = CancelarViajeUiState(cargando = false, viaje = viaje, pago = pago, vencido = vencido, reembolso = "52000")

    private class Avisos {
        val motivos = mutableListOf<String>()
        val reembolsos = mutableListOf<String>()
        var confirmaciones = 0
        var reintentos = 0
        var vueltas = 0
    }

    private fun mostrar(estado: CancelarViajeUiState, avisos: Avisos = Avisos()): Avisos {
        compose.setContent {
            BelmonteTheme {
                CancelarViajeContenido(
                    estado = estado,
                    onMotivo = { avisos.motivos += it },
                    onReembolso = { avisos.reembolsos += it },
                    onConfirmar = { avisos.confirmaciones++ },
                    onReintentar = { avisos.reintentos++ },
                    onVolver = { avisos.vueltas++ },
                )
            }
        }
        return avisos
    }

    private fun texto(id: Int) = textos.getString(id)
    private fun chip(id: Int) = textos.getString(R.string.chip_descripcion, textos.getString(id))
    private fun campo(etiqueta: Int) = compose.onNode(hasText(texto(etiqueta)) and hasSetTextAction())

    @Test
    fun muestraElViajeElSolicitanteElRetrasoYElPago() {
        mostrar(estado())

        compose.onNodeWithText("TR-1038 · Luis Pino").assertExists()
        compose.onAllNodesWithContentDescription(chip(R.string.estado_viaje_pendiente)).assertCountEquals(1)
        compose.onAllNodesWithContentDescription(chip(R.string.etiqueta_vencido)).assertCountEquals(1)
        compose.onAllNodesWithContentDescription(chip(R.string.estado_pago_pagado)).assertCountEquals(1)
        compose.onNodeWithText("24-10-2026 · 06:30").assertExists()
        compose.onNodeWithText("\$52.000").assertExists()
    }

    @Test
    fun vencidoConPagoPagadoPideElMontoYLoExplica() {
        mostrar(estado())

        compose.onNodeWithText(texto(R.string.cancelar_viaje_vencido)).assertExists()
        campo(R.string.campo_reembolso).assertExists()
        campo(R.string.campo_motivo).assertExists()
    }

    @Test
    fun alDiaNoPideMontoYAvisaQueSeReembolsaTodo() {
        mostrar(estado(vencido = false))

        compose.onNodeWithText(textos.getString(R.string.cancelar_viaje_reembolso_total, "\$52.000")).assertExists()
        campo(R.string.campo_reembolso).assertDoesNotExist()
        compose.onAllNodesWithContentDescription(chip(R.string.etiqueta_vencido)).assertCountEquals(0)
    }

    @Test
    fun conUnPagoPendienteAvisaQueSeAnulaYNoPideMonto() {
        mostrar(estado(pago = pago(EstadoPago.PENDIENTE)))

        compose.onNodeWithText(texto(R.string.cancelar_viaje_anula_pago)).assertExists()
        campo(R.string.campo_reembolso).assertDoesNotExist()
    }

    @Test
    fun conUnPagoTerminalElPagoNoSeModifica() {
        mostrar(estado(pago = pago(EstadoPago.RECHAZADO)))

        compose.onNodeWithText(texto(R.string.cancelar_viaje_pago_sin_cambio)).assertExists()
        campo(R.string.campo_reembolso).assertDoesNotExist()
    }

    @Test
    fun sinPagoLoDiceYSoloPideElMotivo() {
        mostrar(estado(viaje = viaje(hashPago = null), pago = null))

        compose.onNodeWithText(texto(R.string.aviso_sin_pago)).assertExists()
        campo(R.string.campo_reembolso).assertDoesNotExist()
        campo(R.string.campo_motivo).assertExists()
        compose.onNodeWithText(texto(R.string.accion_confirmar)).assertIsEnabled()
    }

    @Test
    fun escribirEnLosCamposAvisa() {
        val avisos = mostrar(estado())

        campo(R.string.campo_motivo).performTextInput("Nadie lo atendió")
        campo(R.string.campo_reembolso).performTextReplacement("20000")

        assertEquals(listOf("Nadie lo atendió"), avisos.motivos)
        assertEquals(listOf("20000"), avisos.reembolsos)
    }

    @Test
    fun confirmarYVolverAvisan() {
        val avisos = mostrar(estado())

        compose.onNodeWithText(texto(R.string.accion_confirmar)).assertIsEnabled().performClick()
        compose.onNodeWithText(texto(R.string.accion_volver)).performClick()

        assertEquals(1, avisos.confirmaciones)
        assertEquals(1, avisos.vueltas)
    }

    @Test
    fun cadaErrorSaleBajoSuCampo() {
        mostrar(
            estado().copy(
                errores = mapOf(
                    CAMPO_MOTIVO to "Debe tener entre 5 y 200 caracteres.",
                    CAMPO_REEMBOLSO to "Debe estar entre \$0 y \$52.000.",
                ),
            ),
        )

        compose.onNodeWithText("Debe tener entre 5 y 200 caracteres.").assertExists()
        compose.onNodeWithText("Debe estar entre \$0 y \$52.000.").assertExists()
    }

    @Test
    fun mientrasEnviaNoSePuedeConfirmarNiEscribir() {
        mostrar(estado().copy(enviando = true, motivo = "Nadie lo atendió"))

        compose.onNodeWithText(texto(R.string.accion_confirmar)).assertIsNotEnabled()
        campo(R.string.campo_motivo).assertIsNotEnabled()
    }

    @Test
    fun unViajeQueYaNoSePuedeCancelarMuestraElAvisoYNoSePuedeConfirmar() {
        mostrar(estado(viaje = viaje(EstadoViaje.EN_CURSO)))

        compose.onNodeWithText(texto(R.string.cancelar_viaje_no_disponible)).assertExists()
        campo(R.string.campo_motivo).assertDoesNotExist()
        compose.onNodeWithText(texto(R.string.accion_confirmar)).assertIsNotEnabled()
    }

    @Test
    fun unConflictoSeMuestraSinOfrecerReintentar() {
        mostrar(estado().copy(error = ErrorApp.Conflicto("El viaje está EN_CURSO")))

        compose.onNodeWithTag("t06_error").assertExists()
        compose.onNodeWithText("El viaje está EN_CURSO").assertExists()
        compose.onNodeWithText(texto(R.string.accion_reintentar)).assertDoesNotExist()
    }

    @Test
    fun unPagoQueNoSePudoLeerOfreceReintentarYNoDejaConfirmar() {
        val avisos = mostrar(estado(pago = null).copy(error = ErrorApp.SinConexion))

        compose.onNodeWithText(texto(R.string.error_sin_conexion)).assertExists()
        compose.onNodeWithText(texto(R.string.accion_confirmar)).assertIsNotEnabled()
        compose.onNodeWithText(texto(R.string.accion_reintentar)).performClick()

        assertEquals(1, avisos.reintentos)
    }

    @Test
    fun unViajeQueNoSeEncuentraOfreceReintentar() {
        val avisos = mostrar(CancelarViajeUiState(cargando = false, error = ErrorApp.NoEncontrado))

        compose.onNodeWithTag("t06_error").assertExists()
        compose.onNodeWithText(texto(R.string.error_no_encontrado)).assertExists()
        compose.onNodeWithText(texto(R.string.accion_reintentar)).performClick()

        assertEquals(1, avisos.reintentos)
        compose.onAllNodesWithTag("t06_contenido").assertCountEquals(0)
    }
}
