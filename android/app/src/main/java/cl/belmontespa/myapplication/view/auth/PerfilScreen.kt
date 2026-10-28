package cl.belmontespa.myapplication.view.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.navigation.PantallaDemo
import cl.belmontespa.myapplication.util.Formatos
import cl.belmontespa.myapplication.view.components.Accion
import cl.belmontespa.myapplication.view.components.ContenedorPantalla
import cl.belmontespa.myapplication.view.components.Dato
import cl.belmontespa.myapplication.view.components.TipoBoton
import cl.belmontespa.myapplication.viewmodel.auth.PerfilViewModel

@Composable
fun PerfilScreen(
    pantalla: PantallaDemo,
    onNavegar: (PantallaDemo, String) -> Unit,
    onVolver: (() -> Unit)?,
    onCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.cargar() }
    ContenedorPantalla(
        titulo = stringResource(pantalla.titulo),
        cargando = estado.cargando,
        modificado = false,
        onVolver = onVolver,
        error = estado.error,
        onReintentar = { viewModel.cargar(forzar = true) },
    ) {
        estado.cuenta?.let { cuenta ->
            Dato(R.string.campo_nombre, cuenta.nombre)
            Dato(R.string.campo_correo, cuenta.correo)
            Dato(R.string.campo_telefono, cuenta.telefono?.let(Formatos::telefono))
            Dato(R.string.campo_empresa, cuenta.razonSocial)
            Dato(R.string.campo_rut_empresa, cuenta.rutEmpresa?.let(Formatos::rut))
            Dato(R.string.campo_registrado, Formatos.fechaHoraDetalle(cuenta.fechaRegistro))
            if (cuenta.rol != Rol.CONDUCTOR) {
                Accion(R.string.accion_cambiar_password, { onNavegar(PantallaDemo.CAMBIAR_PASSWORD, "") }, TipoBoton.SECUNDARIO)
            }
            if (cuenta.rol == Rol.SOLICITANTE) {
                Accion(R.string.login_abordar_viaje, { onNavegar(PantallaDemo.ABORDAR, "") }, TipoBoton.SECUNDARIO)
            }
            if (cuenta.rol == Rol.ADMINISTRADOR) {
                Accion(R.string.t06_registrar_admin, { onNavegar(PantallaDemo.REGISTRAR_ADMIN, "") }, TipoBoton.SECUNDARIO)
                Accion(R.string.accion_restablecer_password, { onNavegar(PantallaDemo.RECUPERAR, "") }, TipoBoton.SECUNDARIO)
                Accion(R.string.t06_cuentas_pendientes, { onNavegar(PantallaDemo.CUENTAS_PENDIENTES, "") }, TipoBoton.SECUNDARIO)
            }
            if (BuildConfig.MODO_LOCAL) {
                Accion(R.string.accion_ver_escenarios, { onNavegar(PantallaDemo.NOTIFICACION, "") }, TipoBoton.SECUNDARIO)
            }
        }
        Accion(R.string.accion_cerrar_sesion, onCerrarSesion, TipoBoton.SECUNDARIO)
    }
}
