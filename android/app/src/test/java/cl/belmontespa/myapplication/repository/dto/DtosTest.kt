package cl.belmontespa.myapplication.repository.dto

import cl.belmontespa.myapplication.model.Catalogos
import cl.belmontespa.myapplication.model.EstadoPago
import cl.belmontespa.myapplication.model.MedioPago
import cl.belmontespa.myapplication.model.TipoRecibo
import cl.belmontespa.myapplication.model.Ubicacion
import cl.belmontespa.myapplication.repository.api.ApiClient
import java.time.LocalDateTime
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtosTest {

    private val json = ApiClient.json

    private val viaje = """
        {"folio":"TR-1042","estado":"ASIGNADO","correoSolicitante":"s@x.cl","nombreSolicitante":"Sara","telefonoSolicitante":null,
         "razonSocial":"Acme","origen":{"direccion":"Aeropuerto","comuna":"Pudahuel","region":"Metropolitana","latitud":-33.393,"longitud":-70.786},
         "destino":{"direccion":"Hotel","comuna":"Providencia","region":"Metropolitana","latitud":-33.426,"longitud":-70.617},
         "fhCreacion":"2026-11-10T09:00:00","fhAgenda":"2026-11-14T14:30:00","fhAsignacion":"2026-11-10T09:15:00.123456",
         "fhInicio":null,"fhFin":null,"numeroVuelo":"LA800","cantidadPasajeros":2,"correoConductor":"c@x.cl","nombreConductor":"Carlos",
         "patenteMovil":"KXJR42","hashPago":null,"estadoPago":null,"motivo":null,"campoNuevo":"se ignora",
         "pasajeros":[{"dniHash":"h1","nombre":"Ana","fhAbordaje":"2026-11-14T14:20:00"},{"dniHash":"h2","nombre":null,"fhAbordaje":null}]}
    """.trimIndent()

    @Test
    fun unViajeConFraccionesDeSegundoYCamposNuevosSeLee() {
        val respuesta = json.decodeFromString<ViajeResponse>(viaje)

        assertEquals(LocalDateTime.of(2026, 11, 10, 9, 15, 0, 123_456_000), respuesta.fhAsignacion)
        assertNull(respuesta.fhInicio)
        assertEquals(2, respuesta.pasajeros.size)
    }

    @Test
    fun unViajeSeConvierteEnEntidadesParaLaCache() {
        val guardable = json.decodeFromString<ViajeResponse>(viaje).aGuardable(descargadoEn = 99L, nombresConocidos = mapOf("h2" to "Beto"))

        assertEquals("TR-1042", guardable.viaje.folio)
        assertEquals(1042L, guardable.viaje.folioCorrelativo)
        assertEquals(99L, guardable.viaje.descargadoEn)
        assertEquals("2026-11-10T09:15:00", guardable.viaje.fhAsignacion)
        assertEquals(-33.393, guardable.viaje.origenLatitud, 0.0)
        assertEquals(listOf("Ana", "Beto"), guardable.pasajeros.map { it.nombre })
        assertEquals("2026-11-14T14:20:00", guardable.detalles.first { it.dniHash == "h1" }.fhAbordaje)
        assertNull(guardable.detalles.first { it.dniHash == "h2" }.fhAbordaje)
    }

    @Test
    fun unaUbicacionSeEnviaConSeisDecimales() {
        val dto = Ubicacion("Casa", "Ñuñoa", "Metropolitana", -33.426100000000005, -70.61700000000002).aDto()

        assertEquals(-33.4261, dto.latitud, 0.0)
        assertEquals(-70.617, dto.longitud, 0.0)
    }

    @Test
    fun laFechaSeEscribeSinFraccionesNiZona() {
        val pedido = ViajeRequest(
            LocalDateTime.of(2026, 11, 14, 14, 30, 0, 500),
            Ubicacion("A", "B", "C", 1.0, 2.0).aDto(),
            Ubicacion("D", "E", "F", 3.0, 4.0).aDto(),
            numeroVuelo = null,
        )

        val texto = json.encodeToString(pedido)

        assertTrue(texto, "\"fhAgenda\":\"2026-11-14T14:30:00\"" in texto)
        assertFalse(texto, "numeroVuelo" in texto)
    }

    @Test
    fun unPerfilSinClaveDePasswordNoLaEnvia() {
        val texto = json.encodeToString(UsuarioRequest("Ana", "ana@x.cl", null))

        assertEquals("""{"nombre":"Ana","correo":"ana@x.cl"}""", texto)
    }

    @Test
    fun elCambioDeContrasenaLlevaLaNuevaYLaActual() {
        val texto = json.encodeToString(UsuarioRequest("Ana", "ana@x.cl", "+56911112222", "Nueva1234", "Vieja1234"))

        assertTrue(texto, "\"password\":\"Nueva1234\"" in texto && "\"passwordActual\":\"Vieja1234\"" in texto)
    }

    @Test
    fun unPagoConsultadoSeConvierteALosEnumsDeLaApp() {
        val pago = json.decodeFromString<PagoResponse>(
            """{"hash":"abc","folio":"TR-1042","monto":45000,"estadoPago":"PAGADO","banco":"Banco Estado","codSbif":12,
                "rutEmpresa":"76543210-3","medioPago":"TARJETA_DEBITO","tipoRecibo":"BOLETA","fechaPago":"2026-11-10T10:00:00","montoReembolsado":null}""",
        ).aModelo()

        assertEquals(EstadoPago.PAGADO, pago.estado)
        assertEquals(MedioPago.TARJETA_DEBITO, pago.medioPago)
        assertEquals(TipoRecibo.BOLETA, pago.tipoRecibo)
        assertNull(pago.montoReembolsado)
    }

    @Test
    fun loscatalogosIgnoranLosValoresQueLaAppNoConoce() {
        val catalogos: Catalogos = json.decodeFromString<CatalogosResponse>(
            """{"bancos":[{"nombre":"Banco Estado","codSbif":12}],"mediosPago":["EFECTIVO","CRIPTO"],"tiposRecibo":["FACTURA"],
                "regiones":[{"nombre":"Valparaíso","comunas":["Viña del Mar"]}]}""",
        ).aModelo()

        assertEquals(listOf(MedioPago.EFECTIVO), catalogos.mediosPago)
        assertEquals(listOf(TipoRecibo.FACTURA), catalogos.tiposRecibo)
        assertEquals(12, catalogos.bancos.single().codSbif)
    }

    @Test
    fun elLoginSeLeConLaDuracionEnSegundos() {
        val login = json.decodeFromString<LoginResponse>(
            """{"accessToken":"jwt","tokenType":"Bearer","expiresIn":28800,"correo":"a@x.cl","rol":"CONDUCTOR","cambioPasswordRequerido":false}""",
        )

        assertEquals(28800L, login.expiresIn)
        assertFalse(login.cambioPasswordRequerido)
    }
}
