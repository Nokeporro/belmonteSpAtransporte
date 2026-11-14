package cl.belmontespa.myapplication.sync

import cl.belmontespa.myapplication.model.EstadoEnvio
import cl.belmontespa.myapplication.model.EstadoViaje
import cl.belmontespa.myapplication.model.OperacionPendienteEntity
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.repository.api.ApiClient
import cl.belmontespa.myapplication.session.SesionStore
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

internal const val CONDUCTOR = "ireyes@belmontespa.cl"

internal fun sesionDeConductor() = Sesion("jwt", CONDUCTOR, Rol.CONDUCTOR, System.currentTimeMillis() + 3_600_000)

internal fun apuntarA(sesiones: SesionStore, url: String) =
    ApiClient.reemplazarParaPruebas(sesiones, ApiClient.crear(sesiones, url))

internal fun operacion(
    id: String,
    folio: String,
    destino: EstadoViaje,
    creadoEn: Long,
    cuenta: String = CONDUCTOR,
    envio: EstadoEnvio = EstadoEnvio.PENDIENTE,
    mensaje: String? = null,
) = OperacionPendienteEntity(id, cuenta, folio, destino.name, creadoEn, envio.name, mensaje)

internal fun MockWebServer.responder(codigo: Int, cuerpo: String = "") {
    enqueue(MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo))
}

internal fun viajeJson(folio: String, estado: String) = """
    {"folio":"$folio","estado":"$estado","correoSolicitante":"crios@andesconsult.cl","nombreSolicitante":"Constanza Ríos",
     "telefonoSolicitante":null,"razonSocial":"Andes Consultores SpA",
     "origen":{"direccion":"Aeropuerto","comuna":"Pudahuel","region":"Metropolitana de Santiago","latitud":-33.392975,"longitud":-70.785803},
     "destino":{"direccion":"Av. Apoquindo 5400","comuna":"Las Condes","region":"Metropolitana de Santiago","latitud":-33.41152,"longitud":-70.575384},
     "fhCreacion":"2026-11-13T09:00:00","fhAgenda":"2026-11-14T10:00:00","fhAsignacion":"2026-11-13T09:30:00",
     "fhInicio":null,"fhFin":null,"numeroVuelo":null,"cantidadPasajeros":0,"correoConductor":"$CONDUCTOR",
     "nombreConductor":"Iván Reyes","patenteMovil":"KXJR42","hashPago":null,"estadoPago":null,"motivo":null,"pasajeros":[]}
""".trimIndent()
