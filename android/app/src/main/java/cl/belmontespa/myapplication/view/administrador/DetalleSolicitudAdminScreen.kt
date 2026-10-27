package cl.belmontespa.myapplication.view.administrador

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.arqueo
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.ArqueoDeAbordaje
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
import cl.belmontespa.myapplication.viewmodel.administrador.DetalleSolicitudAdminViewModel

/**
 * «Detalle de solicitud» del Administrador; `pantalla` distingue la variante «Solicitud sin pasajeros» (HU04), que es
 * la misma pantalla sin «Asignar» y con el aviso de que faltan pasajeros.
 */
@Composable
fun DetalleSolicitudAdminScreen(
    pantalla: PantallaDemo,
    folio: String,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: () -> Unit,
    viewModel: DetalleSolicitudAdminViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(folio) { viewModel.cargar(folio) }
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
        // Los pasajeros se ven como arqueo (RF29) en los estados que lo tienen; en los demás, solo sus nombres.
        val arqueo = viaje.arqueo()
        if (viaje.pasajeros.isEmpty()) {
            Aviso(stringResource(R.string.aviso_faltan_pasajeros), TonoAviso.AVISO)
        } else if (arqueo != null) {
            ArqueoDeAbordaje(arqueo, viaje.pasajeros)
        } else {
            Bloque(stringResource(R.string.campo_pasajeros)) { viaje.pasajeros.forEach { Text(it.nombre) } }
        }
        // El pago del viaje, también su resultado tras cancelar (README: motivo, pago y arqueo).
        estado.pago?.let { ResumenPago(it) }
        if (editable && !vencido && viaje.cantidadPasajeros > 0) {
            val texto = if (viaje.estado == EstadoViaje.ASIGNADO) R.string.accion_reasignar else R.string.accion_asignar
            Accion(texto, { onNavegar(PantallaDemo.ASIGNAR, folio) }, TipoBoton.SECUNDARIO)
        }
        if (editable) Accion(R.string.t06_cancelar_viaje, { onNavegar(PantallaDemo.CANCELAR_VIAJE, folio) }, TipoBoton.SECUNDARIO)
        if (viaje.hashPago == null && viaje.estado != EstadoViaje.CANCELADO) {
            Accion(R.string.accion_registrar_pago, { onNavegar(PantallaDemo.REGISTRAR_PAGO, folio) }, TipoBoton.SECUNDARIO)
        }
        viaje.hashPago?.let { hash ->
            Accion(R.string.campo_pago, { onNavegar(PantallaDemo.DETALLE_PAGO, hash) }, TipoBoton.SECUNDARIO)
        }
        Dato(R.string.campo_motivo, viaje.motivo)
    }
}
