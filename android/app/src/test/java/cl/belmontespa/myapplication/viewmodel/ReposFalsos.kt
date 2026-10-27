package cl.belmontespa.myapplication.viewmodel

import cl.belmontespa.myapplication.model.Catalogos
import cl.belmontespa.myapplication.model.Conductor
import cl.belmontespa.myapplication.model.Cuenta
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.EstadoConductor
import cl.belmontespa.myapplication.model.EstadoMovil
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.EstadoSesion
import cl.belmontespa.myapplication.model.EstadoSolicitante
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.FiltroViajes
import cl.belmontespa.myapplication.model.Habilitacion
import cl.belmontespa.myapplication.model.Movil
import cl.belmontespa.myapplication.model.NuevaCuenta
import cl.belmontespa.myapplication.model.NuevaSolicitud
import cl.belmontespa.myapplication.model.NuevoMovil
import cl.belmontespa.myapplication.model.NuevoPago
import cl.belmontespa.myapplication.model.NuevoPasajero
import cl.belmontespa.myapplication.model.Pago
import cl.belmontespa.myapplication.model.PasswordTemporal
import cl.belmontespa.myapplication.model.Region
import cl.belmontespa.myapplication.model.RegistroSolicitante
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.model.Solicitante
import cl.belmontespa.myapplication.model.Viaje
import cl.belmontespa.myapplication.repository.AuthRepository
import cl.belmontespa.myapplication.repository.CatalogoRepository
import cl.belmontespa.myapplication.repository.CuentaRepository
import cl.belmontespa.myapplication.repository.FlotaRepository
import cl.belmontespa.myapplication.repository.PagoRepository
import cl.belmontespa.myapplication.repository.ViajeRepository
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Fakes manuales: una operación inesperada falla sin acceder a Room, red ni Android. */
/**
 * Dobles manuales de los Repository: una operación inesperada falla sin acceder a Room, red ni Android.
 * El estado se comparte en este objeto; cada repositorio (`viajes`, `cuentas`, `flota`, `pagos`, `auth`,
 * `catalogos`) es una subclase que sobrescribe lo que usan los ViewModel.
 */
internal class ReposP1Falsos {
    var espera: CompletableDeferred<Unit>? = null
    var fallo: ErrorApp? = null
    var envios = 0
    var solicitud: NuevaSolicitud? = null
    var conductor = Conductor("Conductor", "conductor@test.cl", null, LocalDateTime.of(2026, 10, 22, 12, 0), true, EstadoConductor.DISPONIBLE, 0, null)
    var cuentaConductor: NuevaCuenta? = null
    var passwordConductor: String? = null
    var enviosConductor = 0
    var enviosAdministrador = 0
    var cuentaAdministrador: NuevaCuenta? = null
    var nuevoMovil: NuevoMovil? = null
    var enviosMovil = 0
    val moviles = mutableListOf<Movil>()
    val habilitaciones = mutableListOf<Habilitacion>()
    var filtroHabilitacion: Pair<String?, String?>? = null
    var enviosHabilitacion = 0
    var cambiosActivo = 0
    val viajeActual = MutableStateFlow<Viaje?>(null)
    var pago: Pago? = null
    var errorPago: ErrorApp? = null
    var consultasPago = 0
    var esperaPago: CompletableDeferred<Unit>? = null
    var enviosPago = 0
    var cambiosPago = 0
    var recuperaciones = 0
    var correoRecuperacion: String? = null
    val TEMPORAL = "Abc123xyzQ98"
    var cambiosPassword = 0
    var clavesCambiadas: Pair<String, String>? = null
    var pendientesRevision = emptyList<Solicitante>()
    var errorPendientes: ErrorApp? = null
    var revisionesCuenta = 0
    var revisionCuenta: Pair<String, Boolean>? = null
    var nuevoPago: NuevoPago? = null
    var catalogosPago: Catalogos? = null
    var errorCatalogos: ErrorApp? = null
    var cancelaciones = 0
    var motivoCancelacion: String? = null
    var montoCancelacion: Int? = null
    private fun sinUso(): Nothing = error("Operación no prevista en esta prueba")
    var perfil = Cuenta("Cuenta", "cuenta@test.cl", null, Rol.SOLICITANTE, LocalDateTime.of(2026, 10, 22, 12, 0))
    var enviosPasajero = 0
    var nuevoPasajero: Pair<String, NuevoPasajero>? = null
    var enviosRegistro = 0
    var registro: RegistroSolicitante? = null
    var estadoRegistro = EstadoSolicitante.PENDIENTE

    inner class ViajeFalso : ViajeRepository(soporteVacio()) {
        override suspend fun crearSolicitud(solicitud: NuevaSolicitud): Resultado<String> {
            envios++; this@ReposP1Falsos.solicitud = solicitud; espera?.await()
            return fallo?.let { Resultado.Fallo(it) } ?: Resultado.Exito("TR-2000")
        }
        override fun observarViajes(filtro: FiltroViajes): Flow<List<Viaje>> = flowOf(emptyList())
        override fun observarViaje(folio: String): Flow<Viaje?> = viajeActual
        override suspend fun refrescarViajes(filtro: FiltroViajes): Resultado<Unit> = sinUso()
        override suspend fun refrescarViaje(folio: String): Resultado<Unit> = sinUso()
        override suspend fun agregarPasajero(folio: String, pasajero: NuevoPasajero): Resultado<Unit> {
            enviosPasajero++; nuevoPasajero = folio to pasajero; espera?.await()
            return fallo?.let { Resultado.Fallo(it) } ?: Resultado.Exito(Unit)
        }
        override suspend fun asignar(folio: String, correoConductor: String, patente: String): Resultado<Unit> = sinUso()
        override suspend fun cancelar(folio: String, motivo: String?, montoReembolso: Int?): Resultado<Unit> {
            cancelaciones++; motivoCancelacion = motivo; montoCancelacion = montoReembolso; espera?.await()
            return fallo?.let { Resultado.Fallo(it) } ?: Resultado.Exito(Unit)
        }
        override suspend fun rechazar(folio: String, motivo: String): Resultado<Unit> = sinUso()
        override suspend fun cambiarEstado(folio: String, estado: EstadoViaje): Resultado<Unit> = sinUso()
    }
    val viajes = ViajeFalso()

    inner class CuentaFalso : CuentaRepository(soporteVacio()) {
        override suspend fun obtenerPerfil() = Resultado.Exito(perfil)
        override suspend fun actualizarCuentaPropia(nombre: String, telefono: String?): Resultado<Cuenta> = sinUso()
        override suspend fun cambiarPassword(passwordActual: String, passwordNueva: String): Resultado<Unit> {
            cambiosPassword++; clavesCambiadas = passwordActual to passwordNueva; espera?.await()
            return fallo?.let { Resultado.Fallo(it) } ?: Resultado.Exito(Unit)
        }
        override suspend fun listarSolicitantesPendientes(): Resultado<List<Solicitante>> =
            errorPendientes?.let { Resultado.Fallo(it) } ?: Resultado.Exito(pendientesRevision)
        private suspend fun revisarCuenta(correo: String, aprobar: Boolean): Resultado<Solicitante> {
            revisionesCuenta++; revisionCuenta = correo to aprobar; espera?.await()
            fallo?.let { return Resultado.Fallo(it) }
            val revisada = pendientesRevision.single { it.correo == correo }.copy(
                estado = if (aprobar) EstadoSolicitante.APROBADO else EstadoSolicitante.RECHAZADO)
            pendientesRevision = pendientesRevision.filterNot { it.correo == correo }
            return Resultado.Exito(revisada)
        }
        override suspend fun aprobarSolicitante(correo: String) = revisarCuenta(correo, true)
        override suspend fun rechazarSolicitante(correo: String) = revisarCuenta(correo, false)
        override suspend fun registrarAdministrador(cuenta: NuevaCuenta): Resultado<Cuenta> {
            enviosAdministrador++; cuentaAdministrador = cuenta; espera?.await()
            fallo?.let { return Resultado.Fallo(it) }
            return Resultado.Exito(Cuenta(cuenta.nombre.trim(), cl.belmontespa.myapplication.util.Validaciones.correo(cuenta.correo),
                cl.belmontespa.myapplication.util.Validaciones.telefono(cuenta.telefono), Rol.ADMINISTRADOR, perfil.fechaRegistro))
        }
    }
    val cuentas = CuentaFalso()

    inner class FlotaFalso : FlotaRepository(soporteVacio()) {
        override suspend fun listarConductores() = Resultado.Exito(listOf(conductor))
        override suspend fun obtenerConductor(correo: String) = Resultado.Exito(conductor)
        override suspend fun registrarConductor(cuenta: NuevaCuenta): Resultado<Conductor> {
            enviosConductor++; cuentaConductor = cuenta; espera?.await()
            return fallo?.let { Resultado.Fallo(it) } ?: Resultado.Exito(conductor)
        }
        override suspend fun actualizarConductor(correo: String, nombre: String, telefono: String?, password: String?): Resultado<Conductor> {
            enviosConductor++; passwordConductor = password; espera?.await()
            if (fallo != null) return Resultado.Fallo(fallo!!)
            conductor = conductor.copy(nombre = nombre.trim(), telefono = telefono)
            return Resultado.Exito(conductor)
        }
        override suspend fun cambiarActivoConductor(correo: String, activo: Boolean): Resultado<Conductor> {
            cambiosActivo++; espera?.await()
            fallo?.let { return Resultado.Fallo(it) }
            conductor = conductor.copy(activo = activo)
            return Resultado.Exito(conductor)
        }
        override suspend fun listarMoviles() = Resultado.Exito(moviles.toList())
        override suspend fun obtenerMovil(patente: String): Resultado<Movil> = moviles.find { it.patente == patente }
            ?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ErrorApp.NoEncontrado)
        override suspend fun registrarMovil(movil: NuevoMovil): Resultado<Movil> {
            enviosMovil++; nuevoMovil = movil; espera?.await()
            fallo?.let { return Resultado.Fallo(it) }
            val registrado = Movil(movil.patente, movil.capacidad, movil.gpsSerial, true, EstadoMovil.DISPONIBLE, "0123456789ABCDEF")
            moviles.add(registrado)
            return Resultado.Exito(registrado)
        }
        override suspend fun cambiarActivoMovil(patente: String, activo: Boolean): Resultado<Movil> {
            cambiosActivo++; espera?.await()
            fallo?.let { return Resultado.Fallo(it) }
            val guardado = moviles.first { it.patente == patente }.copy(activo = activo)
            moviles[moviles.indexOfFirst { it.patente == patente }] = guardado
            return Resultado.Exito(guardado)
        }
        override suspend fun listarHabilitaciones(patente: String?, correo: String?): Resultado<List<Habilitacion>> {
            filtroHabilitacion = patente to correo
            return Resultado.Exito(habilitaciones.filter { (patente == null || it.patente == patente) && (correo == null || it.correoConductor == correo) })
        }
        override suspend fun habilitar(patente: String, correoConductor: String): Resultado<Habilitacion> {
            enviosHabilitacion++; espera?.await()
            fallo?.let { return Resultado.Fallo(it) }
            val movil = moviles.first { it.patente == patente }
            val habilitacion = habilitaciones.find { it.patente == patente && it.correoConductor == correoConductor }
                ?: Habilitacion(patente, movil.capacidad, movil.estado, movil.activo, correoConductor, conductor.nombre, conductor.estado, conductor.activo)
                    .also(habilitaciones::add)
            return Resultado.Exito(habilitacion)
        }
    }
    val flota = FlotaFalso()

    inner class PagoFalso : PagoRepository(soporteVacio()) {
        override suspend fun obtenerPago(hash: String): Resultado<Pago> {
            consultasPago++; esperaPago?.await()
            return errorPago?.let { Resultado.Fallo(it) }
                ?: pago?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ErrorApp.NoEncontrado)
        }
        override suspend fun registrarPago(pago: NuevoPago): Resultado<Pago> {
            enviosPago++; nuevoPago = pago; espera?.await()
            fallo?.let { return Resultado.Fallo(it) }
            val guardado = Pago("0123456789abcdef0123456789abcdef", pago.folio, pago.monto, EstadoPago.PENDIENTE,
                "Banco", pago.codSbif, "12345678-5", pago.medioPago, pago.tipoRecibo, null, null)
            this@ReposP1Falsos.pago = guardado
            return Resultado.Exito(guardado)
        }
        override suspend fun cambiarEstadoPago(hash: String, estado: EstadoPago): Resultado<Pago> {
            cambiosPago++; espera?.await()
            fallo?.let { return Resultado.Fallo(it) }
            val guardado = pago!!.copy(estado = estado,
                fechaPago = if (estado == EstadoPago.PAGADO) LocalDateTime.of(2026, 10, 23, 12, 0) else pago!!.fechaPago)
            pago = guardado
            return Resultado.Exito(guardado)
        }
    }
    val pagos = PagoFalso()

    inner class AuthFalso : AuthRepository(soporteVacio()) {
        override suspend fun iniciarSesion(correo: String, password: String): Resultado<EstadoSesion> = fallo?.let { Resultado.Fallo(it) }
            ?: Resultado.Exito(EstadoSesion.Autenticada(Sesion("demo", correo, Rol.SOLICITANTE, Long.MAX_VALUE)))
        override fun observarSesion(): Flow<EstadoSesion> = flowOf(EstadoSesion.SinSesion)
        override suspend fun registrarSolicitante(registro: RegistroSolicitante): Resultado<Solicitante> {
            enviosRegistro++; this@ReposP1Falsos.registro = registro; espera?.await()
            fallo?.let { return Resultado.Fallo(it) }
            return Resultado.Exito(Solicitante(registro.nombre, registro.correo, registro.telefono, perfil.fechaRegistro,
                registro.razonSocial, registro.rutEmpresa, estadoRegistro))
        }
        override suspend fun recuperarPassword(correo: String): Resultado<PasswordTemporal> {
            recuperaciones++; correoRecuperacion = correo; espera?.await()
            return fallo?.let { Resultado.Fallo(it) } ?: Resultado.Exito(PasswordTemporal(correo, TEMPORAL, perfil.fechaRegistro.plusHours(24)))
        }
        override suspend fun actualizarTokenFcm(token: String): Resultado<Unit> = sinUso()
        override suspend fun cerrarSesion(): Resultado<Unit> = sinUso()
    }
    val auth = AuthFalso()

    inner class CatalogoFalso : CatalogoRepository() {
        override suspend fun obtenerCatalogos(): Resultado<Catalogos> = errorCatalogos?.let { Resultado.Fallo(it) }
            ?: Resultado.Exito(catalogosPago ?: Catalogos(emptyList(), emptyList(), emptyList(), listOf(Region("Metropolitana", listOf("Providencia", "Las Condes")))))
    }
    val catalogos = CatalogoFalso()
}
