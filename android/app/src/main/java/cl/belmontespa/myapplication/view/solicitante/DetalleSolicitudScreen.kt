package cl.belmontespa.myapplication.view.solicitante

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.cancelablePorSolicitante
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.Aviso
import cl.belmontespa.myapplication.view.components.Bloque
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.EstadoChip
import cl.belmontespa.myapplication.view.components.EstiloChip
import cl.belmontespa.myapplication.view.components.ResumenPago
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.view.components.TonoAviso
import cl.belmontespa.myapplication.view.components.estiloChip
import cl.belmontespa.myapplication.view.components.etiqueta
import cl.belmontespa.myapplication.view.components.rememberAhora
import cl.belmontespa.myapplication.viewmodel.solicitante.DetalleSolicitudViewModel

/**
 * «Detalle de solicitud» del Solicitante; `pantalla` distingue la variante «Solicitud sin pasajeros», que es la misma
 * pantalla con el aviso de que faltan pasajeros.
 */
@Composable
fun DetalleSolicitudScreen(
    pantalla: PantallaDemo,
    folio: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: DetalleSolicitudViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(folio) { viewModel.cargar(folio) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.alVolverAPrimerPlano() }
    val ahora = rememberAhora(viewModel::ahora)
    ContenedorPantalla(
        titulo = stringResource(pantalla.titulo),
        cargando = estado.cargando,
        modificado = false,
        onVolver = onVolver,
        error = estado.error,
        onReintentar = viewModel::reintentar,
    ) {
        val viaje = estado.viaje ?: return@ContenedorPantalla
        val editable = viaje.estado == EstadoViaje.PENDIENTE || viaje.estado == EstadoViaje.ASIGNADO
        val vencido = editable && ahora.isAfter(viaje.fhAgenda.plusHours(1))
        Dato(R.string.campo_folio, viaje.folio)
        EstadoChip(stringResource(viaje.estado.etiqueta), viaje.estado.estiloChip)
        if (editable && ahora.isAfter(viaje.fhAgenda)) {
            EstadoChip(
                stringResource(if (vencido) R.string.etiqueta_vencido else R.string.etiqueta_atrasado),
                if (vencido) EstiloChip.PELIGRO_BORDE else EstiloChip.AVISO,
            )
        }
        Dato(R.string.campo_fecha_agenda, Formatos.fechaHoraDetalle(viaje.fhAgenda))
        Dato(R.string.campo_origen, viaje.origen.direccion)
        Dato(R.string.campo_destino, viaje.destino.direccion)
        Dato(R.string.campo_nombre, viaje.nombreSolicitante)
        Dato(R.string.campo_empresa, viaje.razonSocial)
        Dato(R.string.campo_telefono, viaje.telefonoSolicitante?.let(Formatos::telefono))
        Dato(R.string.campo_vuelo, viaje.numeroVuelo)
        Dato(R.string.campo_conductor, viaje.nombreConductor)
        Dato(R.string.campo_patente, viaje.patenteMovil)
        if (viaje.pasajeros.isEmpty()) {
            Aviso(stringResource(R.string.aviso_faltan_pasajeros), TonoAviso.AVISO)
        } else {
            Bloque(stringResource(R.string.campo_pasajeros)) {
                viaje.pasajeros.forEach { pasajero ->
                    Text(pasajero.nombre)
                    val abordo = pasajero.fhAbordaje != null
                    EstadoChip(
                        stringResource(if (abordo) R.string.etiqueta_abordado else R.string.etiqueta_por_abordar),
                        if (abordo) EstiloChip.CORRECTO else EstiloChip.NEUTRO,
                    )
                }
            }
        }
        when {
            viaje.hashPago == null -> Text(stringResource(R.string.aviso_sin_pago))
            estado.cargandoPago -> Text(stringResource(R.string.pago_consulta_cargando))
            estado.errorPago != null -> {
                Aviso(stringResource(R.string.pago_consulta_error), TonoAviso.AVISO)
                Accion(R.string.pago_consulta_reintentar, viewModel::reintentar, TipoBoton.SECUNDARIO)
            }
            else -> estado.pago?.let { Column(Modifier.testTag("pago_resumen")) { ResumenPago(it) } }
        }
        if (editable) {
            Accion(R.string.accion_agregar_pasajero, { onNavegar(PantallaDemo.AGREGAR_PASAJERO, folio) }, TipoBoton.SECUNDARIO)
        }
        if (viaje.cancelablePorSolicitante(ahora)) {
            Accion(R.string.accion_cancelar_solicitud, { onNavegar(PantallaDemo.CONFIRMAR_CANCELACION, folio) }, TipoBoton.SECUNDARIO)
        }
        viaje.hashPago?.let { hash ->
            Accion(R.string.pago_consulta_ver_detalle, { onNavegar(PantallaDemo.DETALLE_PAGO, hash) }, TipoBoton.SECUNDARIO)
        }
        Dato(R.string.campo_motivo, viaje.motivo)
    }
}
