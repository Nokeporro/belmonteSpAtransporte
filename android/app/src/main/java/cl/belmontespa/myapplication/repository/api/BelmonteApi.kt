package cl.belmontespa.myapplication.repository.api

import cl.belmontespa.myapplication.repository.dto.AbordajeRequest
import cl.belmontespa.myapplication.repository.dto.AbordajeResponse
import cl.belmontespa.myapplication.repository.dto.ActivoRequest
import cl.belmontespa.myapplication.repository.dto.AdministradorRequest
import cl.belmontespa.myapplication.repository.dto.AsignarRequest
import cl.belmontespa.myapplication.repository.dto.CancelarRequest
import cl.belmontespa.myapplication.repository.dto.CatalogosResponse
import cl.belmontespa.myapplication.repository.dto.ConductorRequest
import cl.belmontespa.myapplication.repository.dto.ConductorResponse
import cl.belmontespa.myapplication.repository.dto.EstadoViajeRequest
import cl.belmontespa.myapplication.repository.dto.HabilitacionRequest
import cl.belmontespa.myapplication.repository.dto.HabilitacionResponse
import cl.belmontespa.myapplication.repository.dto.LoginRequest
import cl.belmontespa.myapplication.repository.dto.LoginResponse
import cl.belmontespa.myapplication.repository.dto.MovilRequest
import cl.belmontespa.myapplication.repository.dto.MovilResponse
import cl.belmontespa.myapplication.repository.dto.PagoEstadoRequest
import cl.belmontespa.myapplication.repository.dto.PagoRequest
import cl.belmontespa.myapplication.repository.dto.PagoResponse
import cl.belmontespa.myapplication.repository.dto.PasajeroRequest
import cl.belmontespa.myapplication.repository.dto.PasajeroResponse
import cl.belmontespa.myapplication.repository.dto.PasajeroViajeRequest
import cl.belmontespa.myapplication.repository.dto.PasajeroViajeResponse
import cl.belmontespa.myapplication.repository.dto.PasswordTemporalResponse
import cl.belmontespa.myapplication.repository.dto.RechazarRequest
import cl.belmontespa.myapplication.repository.dto.RecuperarPasswordRequest
import cl.belmontespa.myapplication.repository.dto.SolicitanteRequest
import cl.belmontespa.myapplication.repository.dto.SolicitanteResponse
import cl.belmontespa.myapplication.repository.dto.TokenFcmRequest
import cl.belmontespa.myapplication.repository.dto.UsuarioRequest
import cl.belmontespa.myapplication.repository.dto.UsuarioResponse
import cl.belmontespa.myapplication.repository.dto.ViajeRequest
import cl.belmontespa.myapplication.repository.dto.ViajeResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface BelmonteApi {

    @POST("auth/login")
    suspend fun login(@Body pedido: LoginRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout()

    @POST("auth/token-fcm")
    suspend fun tokenFcm(@Body pedido: TokenFcmRequest)

    @POST("auth/recuperar-password")
    suspend fun recuperarPassword(@Body pedido: RecuperarPasswordRequest): PasswordTemporalResponse

    @GET("catalogos")
    suspend fun catalogos(): CatalogosResponse

    @GET("usuarios/{correo}")
    suspend fun usuario(@Path("correo") correo: String): UsuarioResponse

    @PUT("usuarios/{correo}")
    suspend fun actualizarUsuario(@Path("correo") correo: String, @Body pedido: UsuarioRequest): UsuarioResponse

    @POST("solicitantes")
    suspend fun registrarSolicitante(@Body pedido: SolicitanteRequest): SolicitanteResponse

    @GET("solicitantes")
    suspend fun solicitantes(@Query("estado") estado: String?): List<SolicitanteResponse>

    @GET("solicitantes/{correo}")
    suspend fun solicitante(@Path("correo") correo: String): SolicitanteResponse

    @POST("solicitantes/{correo}/aprobar")
    suspend fun aprobarSolicitante(@Path("correo") correo: String): SolicitanteResponse

    @POST("solicitantes/{correo}/rechazar")
    suspend fun rechazarSolicitante(@Path("correo") correo: String): SolicitanteResponse

    @POST("administradores")
    suspend fun registrarAdministrador(@Body pedido: AdministradorRequest): UsuarioResponse

    @GET("conductores")
    suspend fun conductores(): List<ConductorResponse>

    @GET("conductores/{correo}")
    suspend fun conductor(@Path("correo") correo: String): ConductorResponse

    @POST("conductores")
    suspend fun registrarConductor(@Body pedido: ConductorRequest): ConductorResponse

    @PUT("conductores/{correo}")
    suspend fun actualizarConductor(@Path("correo") correo: String, @Body pedido: ConductorRequest): ConductorResponse

    @POST("conductores/{correo}/activo")
    suspend fun cambiarActivoConductor(@Path("correo") correo: String, @Body pedido: ActivoRequest): ConductorResponse

    @GET("moviles")
    suspend fun moviles(): List<MovilResponse>

    @GET("moviles/{patente}")
    suspend fun movil(@Path("patente") patente: String): MovilResponse

    @POST("moviles")
    suspend fun registrarMovil(@Body pedido: MovilRequest): MovilResponse

    @POST("moviles/{patente}/activo")
    suspend fun cambiarActivoMovil(@Path("patente") patente: String, @Body pedido: ActivoRequest): MovilResponse

    @GET("habilitaciones-movil-conductor")
    suspend fun habilitaciones(
        @Query("patente") patente: String?,
        @Query("correo") correo: String?,
    ): List<HabilitacionResponse>

    @POST("habilitaciones-movil-conductor")
    suspend fun habilitar(@Body pedido: HabilitacionRequest): HabilitacionResponse

    @GET("pagos/{hash}")
    suspend fun pago(@Path("hash") hash: String): PagoResponse

    @POST("pagos")
    suspend fun registrarPago(@Body pedido: PagoRequest): PagoResponse

    @POST("pagos/{hash}/estado")
    suspend fun cambiarEstadoPago(@Path("hash") hash: String, @Body pedido: PagoEstadoRequest): PagoResponse

    @POST("pasajeros")
    suspend fun registrarPasajero(@Body pedido: PasajeroRequest): PasajeroResponse

    @POST("pasajeros-viaje")
    suspend fun agregarPasajero(@Body pedido: PasajeroViajeRequest): PasajeroViajeResponse

    @GET("viajes")
    suspend fun viajes(@Query("estado") estado: String?, @Query("fecha") fecha: String?): List<ViajeResponse>

    @GET("viajes/{folio}")
    suspend fun viaje(@Path("folio") folio: String): ViajeResponse

    @POST("viajes")
    suspend fun crearViaje(@Body pedido: ViajeRequest): ViajeResponse

    @POST("viajes/{folio}/asignar")
    suspend fun asignar(@Path("folio") folio: String, @Body pedido: AsignarRequest): ViajeResponse

    @POST("viajes/{folio}/estado")
    suspend fun cambiarEstadoViaje(@Path("folio") folio: String, @Body pedido: EstadoViajeRequest): ViajeResponse

    @POST("viajes/{folio}/cancelar")
    suspend fun cancelarComoSolicitante(@Path("folio") folio: String): ViajeResponse

    @POST("viajes/{folio}/cancelar")
    suspend fun cancelarComoAdministrador(@Path("folio") folio: String, @Body pedido: CancelarRequest): ViajeResponse

    @POST("viajes/{folio}/rechazar")
    suspend fun rechazar(@Path("folio") folio: String, @Body pedido: RechazarRequest): ViajeResponse

    @POST("abordajes")
    suspend fun abordar(@Body pedido: AbordajeRequest): AbordajeResponse
}
